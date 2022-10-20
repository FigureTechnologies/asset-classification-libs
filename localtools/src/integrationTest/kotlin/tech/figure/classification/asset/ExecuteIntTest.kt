package tech.figure.classification.asset

import com.fasterxml.jackson.module.kotlin.readValue
import io.provenance.attribute.v1.QueryAttributeRequest
import org.junit.jupiter.api.Test
import tech.figure.classification.asset.client.domain.execute.AddAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.AddAssetVerifierExecute
import tech.figure.classification.asset.client.domain.execute.DeleteAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.OnboardAssetExecute
import tech.figure.classification.asset.client.domain.execute.ToggleAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAccessRoutesExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetVerifierExecute
import tech.figure.classification.asset.client.domain.execute.VerifyAssetExecute
import tech.figure.classification.asset.client.domain.model.AccessRoute
import tech.figure.classification.asset.client.domain.model.AssetDefinition
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.client.domain.model.AssetScopeAttribute
import tech.figure.classification.asset.client.domain.model.EntityDetail
import tech.figure.classification.asset.client.domain.model.FeeDestination
import tech.figure.classification.asset.client.domain.model.OnboardingCost
import tech.figure.classification.asset.client.domain.model.SubsequentClassificationDetail
import tech.figure.classification.asset.client.domain.model.VerifierDetail
import tech.figure.classification.asset.util.extensions.wrapListAc
import testconfiguration.IntTestBase
import testconfiguration.assertions.assertFeePaymentDetailValidity
import testconfiguration.util.AppResources
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ExecuteIntTest : IntTestBase() {
    @Test
    fun `test onboardAsset`() {
        val asset = assetOnboardingService.storeAndOnboardNewAsset()
        val scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuidOrNull(asset.assetUuid, asset.assetType)
        assertNotNull(
            actual = scopeAttribute,
            message = "The asset scope attribute should be available after onboarding the asset",
        )
        testProvenanceScopeAttributeEquality(scopeAttribute)
        assertFails("Attempting to onboard an asset that does not exist should result in an exception") {
            acClient.onboardAsset(
                execute = OnboardAssetExecute(
                    identifier = AssetIdentifier.AssetUuid(UUID.randomUUID()),
                    assetType = "heloc",
                    verifierAddress = AppResources.verifierAccount.bech32Address,
                    addOsGatewayPermission = true,
                ),
                signer = AppResources.assetOnboardingAccount.toAccountSigner(),
            )
        }
    }

    @Test
    fun `test verifyAsset`() {
        val verifyAnAsset: (assetUuid: UUID, assetType: String, success: Boolean) -> Unit = { assetUuid, assetType, success ->
            acClient.verifyAsset(
                execute = VerifyAssetExecute(
                    identifier = AssetIdentifier.AssetUuid(assetUuid),
                    assetType = assetType,
                    success = success,
                    message = "We verified an asset all on our own!",
                    accessRoutes = AccessRoute(route = "someroute", name = "somename").wrapListAc(),
                ),
                signer = AppResources.verifierAccount.toAccountSigner(),
            )
        }
        assertFails("Attempting to verify an asset that does not exist should fail") {
            verifyAnAsset(UUID.randomUUID(), "some type", true)
        }
        val assetType = "payable"
        val firstAsset = assetOnboardingService.storeAndOnboardNewAsset(assetType = assetType)
        assertFeePaymentDetailValidity(asset = firstAsset)
        verifyAnAsset(firstAsset.assetUuid, firstAsset.assetType, true)
        val firstScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = firstAsset.assetUuid,
            assetType = firstAsset.assetType,
        )
        assertEquals(
            expected = true,
            actual = firstScopeAttribute.latestVerificationResult?.success,
            message = "Expected the scope attribute to indicate that the asset was now verified",
        )
        testProvenanceScopeAttributeEquality(firstScopeAttribute)
        assertFails("Attempting to verify an already-verified asset should fail") {
            verifyAnAsset(firstAsset.assetUuid, firstAsset.assetType, true)
        }
        assertFails("Attempting to re-onboard an already successfully verified asset should fail") {
            acClient.onboardAsset(
                execute = OnboardAssetExecute(
                    identifier = AssetIdentifier.AssetUuid(firstAsset.assetUuid),
                    assetType = assetType,
                    verifierAddress = AppResources.verifierAccount.bech32Address,
                    accessRoutes = AccessRoute("some route", "some name").wrapListAc(),
                    addOsGatewayPermission = false,
                ),
                signer = AppResources.assetOnboardingAccount.toAccountSigner(),
            )
        }
        val secondAsset = assetOnboardingService.storeAndOnboardNewAsset(assetType = assetType)
        assertFeePaymentDetailValidity(secondAsset)
        verifyAnAsset(secondAsset.assetUuid, secondAsset.assetType, false)
        // After verifying an asset as success = false, the asset should be allowed to onboard again
        acClient.onboardAsset(
            execute = OnboardAssetExecute(
                identifier = AssetIdentifier.AssetUuid(secondAsset.assetUuid),
                assetType = assetType,
                verifierAddress = AppResources.verifierAccount.bech32Address,
                accessRoutes = AccessRoute("some route", "some name").wrapListAc(),
                addOsGatewayPermission = true,
            ),
            signer = AppResources.assetOnboardingAccount.toAccountSigner(),
        )
        assertFeePaymentDetailValidity(secondAsset, isRetry = true)
        // Re-verify after the re-onboard process runs
        verifyAnAsset(secondAsset.assetUuid, secondAsset.assetType, true)
        val secondScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = secondAsset.assetUuid,
            assetType = secondAsset.assetType,
        )
        assertEquals(
            expected = true,
            actual = secondScopeAttribute.latestVerificationResult?.success,
            message = "The re-onboard and re-verification process should mark the asset as successfully verified",
        )
        testProvenanceScopeAttributeEquality(secondScopeAttribute)
    }

    @Test
    fun `test multiple verifications with subsequent classifications and applicable asset type`() {
        val owner = AppResources.assetOnboardingAccount
        // Update the contract to ensure that mortgages have a low subsequent classification cost to differentiate the
        // result from retries (which are free by default).  This also establishes "heloc" as an applicable asset type
        // to show that the subsequent cost is used only when an applicable asset type is used
        acClient.queryAssetDefinitionByAssetType("mortgage").let { mortgageDef ->
            mortgageDef.copy(
                verifiers = mortgageDef.verifiers.singleOrNull()?.copy(
                    subsequentClassificationDetail = SubsequentClassificationDetail(
                        // This number is arbitrary, but it's also important to note that only EVEN numbers are accepted
                        // as onboarding costs, so putting 12345 here would fail
                        cost = OnboardingCost(cost = "123456".toBigInteger()),
                        applicableAssetTypes = listOf("heloc"),
                    )
                )?.let(::listOf)
                    ?: fail("Expected only a single verifier to exist for the mortgage asset definition")
            )
        }.also(::updateAssetDefinition)
        val asset = assetOnboardingService.storeAndOnboardNewAsset(assetType = "heloc", ownerAccount = owner)
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            assetType = "mortgage",
            ownerAccount = owner,
        )
        val preVerifyHelocScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "heloc",
        )
        assertEquals(
            expected = AssetOnboardingStatus.PENDING,
            actual = preVerifyHelocScopeAttribute.onboardingStatus,
            message = "The heloc attribute should indicate that the asset is awaiting verification",
        )
        assertFeePaymentDetailValidity(asset, assetType = "heloc")
        val preVerifyMortgageScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "mortgage",
        )
        assertEquals(
            expected = AssetOnboardingStatus.PENDING,
            actual = preVerifyMortgageScopeAttribute.onboardingStatus,
            message = "The mortgage attribute should indicate that the asset is awaiting verification",
        )
        assertFeePaymentDetailValidity(asset, assetType = "mortgage", isSubsequentClassification = true)
        acClient.verifyAsset(
            execute = VerifyAssetExecute(
                identifier = AssetIdentifier.AssetUuid(value = asset.assetUuid),
                assetType = "heloc",
                success = true,
                message = "Successful heloc verification",
            ),
            signer = AppResources.verifierAccount.toAccountSigner(),
        )
        assertNull(
            actual = acClient.queryFeePaymentsByAssetUuidOrNull(assetUuid = asset.assetUuid, assetType = "heloc"),
            message = "Fee payments should be null for the heloc record after heloc verification runs",
        )
        val postVerifyHelocScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "heloc",
        )
        assertEquals(
            expected = AssetOnboardingStatus.APPROVED,
            actual = postVerifyHelocScopeAttribute.onboardingStatus,
            message = "Expected the onboarding status to show that the heloc attribute has been approved",
        )
        acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "mortgage",
        ).also { mortgageScopeAttribute ->
            assertEquals(
                expected = preVerifyMortgageScopeAttribute,
                actual = mortgageScopeAttribute,
                message = "The mortgage scope attribute should be wholly unchanged by the heloc verification",
            )
            assertFeePaymentDetailValidity(asset, assetType = "mortgage", isSubsequentClassification = true)
        }
        acClient.verifyAsset(
            execute = VerifyAssetExecute(
                identifier = AssetIdentifier.AssetUuid(value = asset.assetUuid),
                assetType = "mortgage",
                success = false,
                message = "Failed because this is a heloc, duh",
            ),
            signer = AppResources.verifierAccount.toAccountSigner(),
        )
        assertNull(
            actual = acClient.queryFeePaymentsByAssetUuidOrNull(assetUuid = asset.assetUuid, assetType = "mortgage"),
            message = "Fee payments should be null for the mortgage record after mortgage verification runs",
        )
        val postVerifyMortgageScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "mortgage",
        )
        assertEquals(
            expected = AssetOnboardingStatus.DENIED,
            actual = postVerifyMortgageScopeAttribute.onboardingStatus,
            message = "Expected the onboarding status to show that the mortgage attribute has been denied",
        )
        assertFails("Attempting to onboarding a second time as heloc should fail because the asset is already approved") {
            acClient.onboardAsset(
                execute = OnboardAssetExecute(
                    identifier = AssetIdentifier.AssetUuid(value = asset.assetUuid),
                    assetType = "heloc",
                    verifierAddress = AppResources.verifierAccount.bech32Address,
                ),
                signer = owner.toAccountSigner(),
            )
        }
        // Onboard a second time as mortgage to show that the retry flow is still allowed in this odd circumstance
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            assetType = "mortgage",
            ownerAccount = owner,
        )
        assertFeePaymentDetailValidity(asset, assetType = "mortgage", isRetry = true)
        val preSecondVerifyMortgageScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "mortgage",
        )
        assertEquals(
            expected = AssetOnboardingStatus.PENDING,
            actual = preSecondVerifyMortgageScopeAttribute.onboardingStatus,
            message = "The mortgage verification should move back to pending status after a second onboard",
        )
        acClient.verifyAsset(
            execute = VerifyAssetExecute(
                identifier = AssetIdentifier.AssetUuid(value = asset.assetUuid),
                assetType = "mortgage",
                success = true,
                message = "Oh, I guess this somehow is a heloc and a mortgage",
            ),
            signer = AppResources.verifierAccount.toAccountSigner(),
        )
        val postSecondVerifyMortgageScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "mortgage",
        )
        assertEquals(
            expected = AssetOnboardingStatus.APPROVED,
            actual = postSecondVerifyMortgageScopeAttribute.onboardingStatus,
            message = "The asset should be moved to approved for mortgage after the second successful verification",
        )
        assertNull(
            actual = acClient.queryFeePaymentsByAssetUuidOrNull(assetUuid = asset.assetUuid, assetType = "mortgage"),
            message = "Fee payments for the mortgage flow should be removed after the second verification",
        )
        acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = "heloc"
        ).also { helocScopeAttribute ->
            assertEquals(
                expected = postVerifyHelocScopeAttribute,
                actual = helocScopeAttribute,
                message = "The heloc scope attribute should be unchanged by all mortgage contract actions",
            )
        }
    }

    @Test
    fun `test subsequent classification with no applicable asset types defined`() {
        val owner = AppResources.assetOnboardingAccount
        // Update the contract to ensure that mortgages have a low subsequent classification cost to differentiate the
        // result from retries (which are free by default).  This also purposefully omits applicable asset types, which
        // will cause the contract to apply its cost to any subsequent onboard
        acClient.queryAssetDefinitionByAssetType("mortgage").let { mortgageDef ->
            mortgageDef.copy(
                verifiers = mortgageDef.verifiers.singleOrNull()?.copy(
                    subsequentClassificationDetail = SubsequentClassificationDetail(
                        // This number is arbitrary, but it's also important to note that only EVEN numbers are accepted
                        // as onboarding costs, so putting 12345 here would fail
                        cost = OnboardingCost(cost = "123456".toBigInteger()),
                    )
                )?.let(::listOf)
                    ?: fail("Expected only a single verifier to exist for the mortgage asset definition")
            )
        }.also(::updateAssetDefinition)
        // Ensure that a new mortgage will not use subsequent classification costs because it has no other asset types
        val nonSubsequentMortgage = assetOnboardingService.storeAndOnboardNewAsset(assetType = "mortgage", ownerAccount = owner)
        assertFeePaymentDetailValidity(nonSubsequentMortgage, assetType = "mortgage")
        // Store a scope first as a heloc to then subsequently classify as mortgage
        val asset = assetOnboardingService.storeAndOnboardNewAsset(assetType = "heloc", ownerAccount = owner)
        // Sanity check that this heloc got normal costs
        assertFeePaymentDetailValidity(asset, assetType = "heloc")
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            assetType = "mortgage",
            ownerAccount = owner,
        )
        // Check then that the mortgage got subsequent costs applied
        assertFeePaymentDetailValidity(asset, assetType = "mortgage", isSubsequentClassification = true)
    }

    @Test
    fun `test subsequent classification as non applicable type uses default costs`() {
        val owner = AppResources.assetOnboardingAccount
        // Update the contract to ensure that mortgages have a low subsequent classification cost to differentiate the
        // result from retries (which are free by default).  This sets the applicable types for using the subsequent
        // costs to be pl only, which will not be used here.
        acClient.queryAssetDefinitionByAssetType("mortgage").let { mortgageDef ->
            mortgageDef.copy(
                verifiers = mortgageDef.verifiers.singleOrNull()?.copy(
                    subsequentClassificationDetail = SubsequentClassificationDetail(
                        // This number is arbitrary, but it's also important to note that only EVEN numbers are accepted
                        // as onboarding costs, so putting 12345 here would fail
                        cost = OnboardingCost(cost = "123456".toBigInteger()),
                        applicableAssetTypes = listOf("pl"),
                    )
                )?.let(::listOf)
                    ?: fail("Expected only a single verifier to exist for the mortgage asset definition")
            )
        }.also(::updateAssetDefinition)
        // Ensure that a new mortgage will not use subsequent classification costs because it has no other asset types
        val nonSubsequentMortgage = assetOnboardingService.storeAndOnboardNewAsset(assetType = "mortgage", ownerAccount = owner)
        assertFeePaymentDetailValidity(nonSubsequentMortgage, assetType = "mortgage")
        // Store a scope first as a heloc to then subsequently classify as mortgage
        val asset = assetOnboardingService.storeAndOnboardNewAsset(assetType = "heloc", ownerAccount = owner)
        // Sanity check that this heloc got normal costs
        assertFeePaymentDetailValidity(asset, assetType = "heloc")
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            assetType = "mortgage",
            ownerAccount = owner,
        )
        // Check then that the mortgage did not get subsequent costs applied
        assertFeePaymentDetailValidity(asset, assetType = "mortgage")
    }

    @Test
    fun `test addAssetDefinition`() {
        addTemporaryAssetDefinition("faketype") { assetDefinition ->
            assertEquals(
                expected = "faketype",
                actual = assetDefinition.assetType,
                message = "Expected the added definition to have the correct name",
            )
            assertFails("Attempting to add an asset definition of an existing type should fail") {
                addTemporaryAssetDefinition("fakeType")
            }
        }
    }

    @Test
    fun `test updateAssetDefinition`() {
        assertFails("Attempting to update an asset definition that does not exist should fail") {
            acClient.updateAssetDefinition(
                execute = UpdateAssetDefinitionExecute(
                    assetType = "some fake type",
                    verifiers = getDefaultVerifierDetail().wrapListAc(),
                    enabled = false,
                ),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        addTemporaryAssetDefinition("faketype", enabled = true) { assetDefinition ->
            assertTrue(
                actual = assetDefinition.enabled,
                message = "Sanity check: Created asset definition should be enabled",
            )
            acClient.updateAssetDefinition(
                execute = UpdateAssetDefinitionExecute(
                    assetType = "faketype",
                    displayName = "some display name",
                    verifiers = assetDefinition.verifiers,
                    enabled = false,
                ),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
            val newDefinition = acClient.queryAssetDefinitionByAssetType("faketype")
            assertFalse(
                actual = newDefinition.enabled,
                message = "The update should successfully disable the asset definition",
            )
            assertEquals(
                expected = "some display name",
                actual = newDefinition.displayName,
                message = "The update should successfully change the display name",
            )
            assertEquals(
                expected = assetDefinition.copy(displayName = "some display name", enabled = false),
                actual = newDefinition,
                message = "The definitions should be identical except for their enabled and displayName properties",
            )
        }
    }

    @Test
    fun `test toggleAssetDefinition`() {
        assertFails("Attempting to toggle an asset definition that does not exist should fail") {
            acClient.toggleAssetDefinition(
                execute = ToggleAssetDefinitionExecute(
                    assetType = "something that is not real",
                    expectedResult = true,
                ),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        addTemporaryAssetDefinition("faketype", enabled = true) { assetDefinition ->
            val toggleDefinition: (expectedState: Boolean) -> Unit = { expectedState ->
                acClient.toggleAssetDefinition(
                    execute = ToggleAssetDefinitionExecute(
                        assetType = assetDefinition.assetType,
                        expectedResult = expectedState,
                    ),
                    signer = AppResources.contractAdminAccount.toAccountSigner(),
                )
            }
            toggleDefinition(false)
            val toggledDefinition = acClient.queryAssetDefinitionByAssetType(assetDefinition.assetType)
            assertFalse(
                actual = toggledDefinition.enabled,
                message = "The toggle should succeed in disabling the asset definition",
            )
            toggleDefinition(true)
            val doubleToggled = acClient.queryAssetDefinitionByAssetType(assetDefinition.assetType)
            assertTrue(
                actual = doubleToggled.enabled,
                message = "The second toggle should return the asset definition to an enabled state",
            )
        }
    }

    @Test
    fun `test addAssetVerifier`() {
        assertFails("Attempting to add an asset verifier to an asset definition that does not exist should fail") {
            acClient.addAssetVerifier(
                execute = AddAssetVerifierExecute(assetType = "notrealatall", verifier = getDefaultVerifierDetail()),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        addTemporaryAssetDefinition("faketype") { assetDefinition ->
            assertFails("Attempting to add a duplicate verifier should fail") {
                acClient.addAssetVerifier(
                    execute = AddAssetVerifierExecute(assetType = "faketype", verifier = getDefaultVerifierDetail()),
                    signer = AppResources.contractAdminAccount.toAccountSigner(),
                )
            }
            val newVerifier = VerifierDetail(
                address = AppResources.assetAdminAccount.bech32Address,
                onboardingCost = "250".toBigInteger(),
                onboardingDenom = "nhash",
                feeDestinations = emptyList(),
            )
            acClient.addAssetVerifier(
                execute = AddAssetVerifierExecute(assetType = "faketype", verifier = newVerifier),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
            val definitionAfterUpdate = acClient.queryAssetDefinitionByAssetType("faketype")
            assertEquals(
                expected = 2,
                actual = definitionAfterUpdate.verifiers.size,
                message = "Expected both verifiers to exist on the asset definition after the add",
            )
            val originalVerifier = assetDefinition.verifiers.single()
            assertEquals(
                expected = originalVerifier,
                actual = definitionAfterUpdate.verifiers.singleOrNull { it.address == originalVerifier.address },
                message = "The original verifier should remain on the asset definition",
            )
            assertEquals(
                expected = newVerifier,
                actual = definitionAfterUpdate.verifiers.singleOrNull { it.address == newVerifier.address },
                message = "The new verifier should be added to the asset definition",
            )
        }
    }

    @Test
    fun `test updateAssetVerifier`() {
        assertFails("Attempting to update an asset verifier for an asset definition that does not exist should fail") {
            acClient.updateAssetVerifier(
                execute = UpdateAssetVerifierExecute(assetType = "faketype", verifier = getDefaultVerifierDetail()),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        addTemporaryAssetDefinition("faketype") { assetDefinition ->
            assertFails("Attempting to update an asset verifier for a verifier detail that does not exist should fail") {
                acClient.updateAssetVerifier(
                    execute = UpdateAssetVerifierExecute(
                        assetType = "faketype",
                        verifier = VerifierDetail(
                            address = AppResources.assetAdminAccount.bech32Address,
                            onboardingCost = "400".toBigInteger(),
                            onboardingDenom = "nhash",
                        )
                    ),
                    signer = AppResources.contractAdminAccount.toAccountSigner(),
                )
            }
            val updatedVerifier = assetDefinition.verifiers.single().copy(onboardingCost = "88383838".toBigInteger())
            acClient.updateAssetVerifier(
                execute = UpdateAssetVerifierExecute(assetType = "faketype", verifier = updatedVerifier),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
            val updatedDefinition = acClient.queryAssetDefinitionByAssetType("faketype")
            assertEquals(
                expected = updatedVerifier,
                actual = updatedDefinition.verifiers.singleOrNull(),
                message = "There should be a single verifier instance after the update, but the count was [${updatedDefinition.verifiers.size}], and the single value should be equal to the sent update",
            )
        }
    }

    @Test
    fun `test updateAccessRoutes`() {
        assertFails("Attempting to update access routes for a scope attribute that does not exist should fail") {
            acClient.updateAccessRoutes(
                execute = UpdateAccessRoutesExecute(
                    identifier = AssetIdentifier.AssetUuid(UUID.randomUUID()),
                    assetType = "sometype",
                    ownerAddress = AppResources.assetOnboardingAccount.bech32Address,
                    accessRoutes = AccessRoute("route", "name").wrapListAc(),
                ),
                signer = AppResources.assetOnboardingAccount.toAccountSigner()
            )
        }
        val asset = assetOnboardingService.storeAndOnboardNewAsset(ownerAccount = AppResources.assetOnboardingAccount)
        assertFails("Attempting to update access routes for an unrelated account should fail") {
            acClient.updateAccessRoutes(
                execute = UpdateAccessRoutesExecute(
                    identifier = AssetIdentifier.AssetUuid(asset.assetUuid),
                    assetType = asset.assetType,
                    ownerAddress = AppResources.assetManagerAccount.bech32Address,
                    accessRoutes = AccessRoute("route", "name").wrapListAc()
                ),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        val scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
            assetUuid = asset.assetUuid,
            assetType = asset.assetType,
        )
        val originalAccessRoutes = scopeAttribute.accessDefinitions.singleOrNull { it.ownerAddress == AppResources.assetOnboardingAccount.bech32Address }?.accessRoutes
        assertNotNull(
            actual = originalAccessRoutes,
            message = "There should be access routes specified for the asset owner after the default onboarding process",
        )
        assertEquals(
            expected = 1,
            actual = originalAccessRoutes.size,
            message = "There should be a single access route for the owner address by default",
        )
        val newAccessRoutes = listOf(
            AccessRoute("route1", "route1"),
            AccessRoute("route2", "route2"),
            AccessRoute("route3", "route3"),
        )
        acClient.updateAccessRoutes(
            execute = UpdateAccessRoutesExecute(
                identifier = AssetIdentifier.AssetUuid(asset.assetUuid),
                assetType = asset.assetType,
                ownerAddress = AppResources.assetOnboardingAccount.bech32Address,
                accessRoutes = newAccessRoutes,
            ),
            signer = AppResources.assetOnboardingAccount.toAccountSigner(),
        )
        acClient.queryAssetScopeAttributeByAssetUuid(assetUuid = asset.assetUuid, assetType = asset.assetType)
            .accessDefinitions
            .singleOrNull { it.ownerAddress == AppResources.assetOnboardingAccount.bech32Address }
            ?.accessRoutes
            ?.also { updatedAccessRoutes ->
                assertEquals(
                    expected = newAccessRoutes.sortedBy { it.route },
                    actual = updatedAccessRoutes.sortedBy { it.route },
                    message = "The updated access routes should be correctly set to the target value",
                )
            }
        // The admin should also be allowed to update access routes
        acClient.updateAccessRoutes(
            execute = UpdateAccessRoutesExecute(
                identifier = AssetIdentifier.AssetUuid(asset.assetUuid),
                assetType = asset.assetType,
                ownerAddress = AppResources.assetOnboardingAccount.bech32Address,
                accessRoutes = originalAccessRoutes,
            ),
            signer = AppResources.contractAdminAccount.toAccountSigner(),
        )
        acClient.queryAssetScopeAttributeByAssetUuid(assetUuid = asset.assetUuid, assetType = asset.assetType)
            .accessDefinitions
            .singleOrNull { it.ownerAddress == AppResources.assetOnboardingAccount.bech32Address }
            ?.accessRoutes
            ?.also { updatedAccessRoutes ->
                assertEquals(
                    expected = originalAccessRoutes.sortedBy { it.route },
                    actual = updatedAccessRoutes.sortedBy { it.route },
                    message = "The updated access routes should now be set back to the original value after the second update",
                )
            }
    }

    @Test
    fun `test deleteAssetDefinition`() {
        assertFails("Attempting to delete an asset definition that does not exist should fail") {
            acClient.deleteAssetDefinition(
                execute = DeleteAssetDefinitionExecute(assetType = "faketype"),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        acClient.addAssetDefinition(
            execute = AddAssetDefinitionExecute(
                assetType = "deleteme",
                verifiers = getDefaultVerifierDetail().wrapListAc(),
                enabled = true,
                // This test simulates how the blockchain operates - the "asset" name is restricted and
                // owned by an external account, so attempting to bind the name via the contract will
                // always fail.  This must explicitly be set to false to avoid errors
                bindName = false,
            ),
            signer = AppResources.contractAdminAccount.toAccountSigner(),
        )
        assertFails("Attempting to delete an asset definition from an account that is not the admin should fail") {
            acClient.deleteAssetDefinition(
                execute = DeleteAssetDefinitionExecute("deleteme"),
                signer = AppResources.assetOnboardingAccount.toAccountSigner(),
            )
        }
        acClient.deleteAssetDefinition(
            execute = DeleteAssetDefinitionExecute("deleteme"),
            signer = AppResources.contractAdminAccount.toAccountSigner(),
        )
        assertNull(
            actual = acClient.queryAssetDefinitionByAssetTypeOrNull("deleteme"),
            message = "The asset definition should be successfully deleted",
        )
    }

    private fun testProvenanceScopeAttributeEquality(scopeAttribute: AssetScopeAttribute) {
        val assetAttributeName = "${scopeAttribute.assetType}.asset"
        val attributeFromProvenance = pbClient.attributeClient.attribute(
            QueryAttributeRequest.newBuilder()
                .setAccount(scopeAttribute.scopeAddress)
                .setName(assetAttributeName)
                .build()
        ).attributesList.singleOrNull()
        assertNotNull(
            actual = attributeFromProvenance,
            message = "The attribute should be tagged on the scope after onboarding",
        )
        val scopeAttributeFromAttribute = objectMapper.readValue<AssetScopeAttribute>(attributeFromProvenance.value.toByteArray())
        assertEquals(
            expected = scopeAttribute,
            actual = scopeAttributeFromAttribute,
            message = "The serialized scope attribute on chain should equate to the value produced in the contract, minus its verifier detail",
        )
    }

    private fun addTemporaryAssetDefinition(
        assetType: String,
        enabled: Boolean = true,
        testFunction: (AssetDefinition) -> Unit = {},
    ) {
        acClient.addAssetDefinition(
            execute = AddAssetDefinitionExecute(
                assetType = assetType,
                verifiers = getDefaultVerifierDetail().wrapListAc(),
                enabled = enabled,
                // This test simulates how the blockchain operates - the "asset" name is restricted and
                // owned by an external account, so attempting to bind the name via the contract will
                // always fail.  This must explicitly be set to false to avoid errors
                bindName = false,
            ),
            signer = AppResources.contractAdminAccount.toAccountSigner(),
        )
        val assetDefinition = acClient.queryAssetDefinitionByAssetType(assetType)
        try {
            testFunction.invoke(assetDefinition)
        } catch (e: Exception) {
            fail("Custom test assertions failed when using addTempraryAssetDefinition", e)
        } finally {
            // Always remove the created asset definition, even if the upstream function fails.  This ensures that
            // failing assertions in a test don't cause orphaned asset definitions to remain and cause all sorts of
            // ripple effects to other tests
            acClient.deleteAssetDefinition(
                execute = DeleteAssetDefinitionExecute(assetType = assetType),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
            assertNull(
                actual = acClient.queryAssetDefinitionByAssetTypeOrNull(assetType),
                message = "Expected the asset definition to be removed after deleting it.  Bad test behavior might occur after this failure",
            )
        }
    }

    private fun getDefaultVerifierDetail(): VerifierDetail = VerifierDetail(
        address = AppResources.verifierAccount.bech32Address,
        onboardingCost = "10".toBigInteger(),
        onboardingDenom = "nhash",
        feeDestinations = FeeDestination(
            address = AppResources.contractAdminAccount.bech32Address,
            feeAmount = "5".toBigInteger(),
            entityDetail = EntityDetail(
                name = "Contract Admin",
                description = "Administrates the contract",
                homeUrl = "http://provenance.io",
                sourceUrl = "http://provenance.io",
            ),
        ).wrapListAc(),
        entityDetail = EntityDetail(
            name = "Provenance Verifier",
            description = "Verifies Test Assets by No-Op",
            homeUrl = "http://provenance.io",
            sourceUrl = "http://provenance.io",
        )
    )
}
