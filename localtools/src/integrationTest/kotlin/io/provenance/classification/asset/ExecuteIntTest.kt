package io.provenance.classification.asset

import com.fasterxml.jackson.module.kotlin.readValue
import io.provenance.attribute.v1.QueryAttributeRequest
import io.provenance.classification.asset.client.domain.execute.AddAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.AddAssetVerifierExecute
import io.provenance.classification.asset.client.domain.execute.BindContractAliasExecute
import io.provenance.classification.asset.client.domain.execute.DeleteAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.OnboardAssetExecute
import io.provenance.classification.asset.client.domain.execute.ToggleAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.UpdateAccessRoutesExecute
import io.provenance.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.UpdateAssetVerifierExecute
import io.provenance.classification.asset.client.domain.execute.VerifyAssetExecute
import io.provenance.classification.asset.client.domain.model.AccessRoute
import io.provenance.classification.asset.client.domain.model.AssetDefinition
import io.provenance.classification.asset.client.domain.model.AssetIdentifier
import io.provenance.classification.asset.client.domain.model.AssetQualifier
import io.provenance.classification.asset.client.domain.model.AssetScopeAttribute
import io.provenance.classification.asset.client.domain.model.EntityDetail
import io.provenance.classification.asset.client.domain.model.FeeDestination
import io.provenance.classification.asset.client.domain.model.ScopeSpecIdentifier
import io.provenance.classification.asset.client.domain.model.VerifierDetail
import io.provenance.classification.asset.util.extensions.wrapListAc
import io.provenance.classification.asset.util.wallet.ProvenanceAccountDetail
import io.provenance.client.protobuf.extensions.resolveAddressForName
import org.junit.jupiter.api.Test
import testconfiguration.IntTestBase
import testconfiguration.util.AppResources
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecuteIntTest : IntTestBase() {
    @Test
    fun `test onboardAsset`() {
        val asset = invoiceOnboardingService.onboardTestAsset()
        val scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuidOrNull(asset.assetUuid)
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
                ),
                signer = AppResources.assetOnboardingAccount.toAccountSigner(),
            )
        }
    }

    @Test
    fun `test verifyAsset`() {
        val verifyAnAsset: (assetUuid: UUID, success: Boolean) -> Unit = { assetUuid, success ->
            acClient.verifyAsset(
                execute = VerifyAssetExecute(
                    identifier = AssetIdentifier.AssetUuid(assetUuid),
                    success = success,
                    message = "We verified an asset all on our own!",
                    accessRoutes = AccessRoute(route = "someroute", name = "somename").wrapListAc(),
                ),
                signer = AppResources.verifierAccount.toAccountSigner(),
            )
        }
        assertFails("Attempting to verify an asset that does not exist should fail") {
            verifyAnAsset(UUID.randomUUID(), true)
        }
        val assetType = "payable"
        val firstAsset = invoiceOnboardingService.onboardTestAsset(assetType = assetType)
        verifyAnAsset(firstAsset.assetUuid, true)
        val firstScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(firstAsset.assetUuid)
        assertEquals(
            expected = true,
            actual = firstScopeAttribute.latestVerificationResult?.success,
            message = "Expected the scope attribute to indicate that the asset was now verified",
        )
        testProvenanceScopeAttributeEquality(firstScopeAttribute)
        assertFails("Attempting to verify an already-verified asset should fail") {
            verifyAnAsset(firstAsset.assetUuid, true)
        }
        assertFails("Attempting to re-onboard an already successfully verified asset should fail") {
            acClient.onboardAsset(
                execute = OnboardAssetExecute(
                    identifier = AssetIdentifier.AssetUuid(firstAsset.assetUuid),
                    assetType = assetType,
                    verifierAddress = AppResources.verifierAccount.bech32Address,
                    accessRoutes = AccessRoute("some route", "some name").wrapListAc(),
                ),
                signer = AppResources.assetOnboardingAccount.toAccountSigner(),
            )
        }
        val secondAsset = invoiceOnboardingService.onboardTestAsset(assetType = assetType)
        verifyAnAsset(secondAsset.assetUuid, false)
        // After verifying an asset as success = false, the asset should be allowed to onboard again
        acClient.onboardAsset(
            execute = OnboardAssetExecute(
                identifier = AssetIdentifier.AssetUuid(secondAsset.assetUuid),
                assetType = assetType,
                verifierAddress = AppResources.verifierAccount.bech32Address,
                accessRoutes = AccessRoute("some route", "some name").wrapListAc(),
            ),
            signer = AppResources.assetOnboardingAccount.toAccountSigner(),
        )
        // Re-verify after the re-onboard process runs
        verifyAnAsset(secondAsset.assetUuid, true)
        val secondScopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(secondAsset.assetUuid)
        assertEquals(
            expected = true,
            actual = secondScopeAttribute.latestVerificationResult?.success,
            message = "The re-onboard and re-verification process should mark the asset as successfully verified",
        )
        testProvenanceScopeAttributeEquality(secondScopeAttribute)
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
                    scopeSpecIdentifier = ScopeSpecIdentifier.Uuid(UUID.randomUUID()),
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
                    scopeSpecIdentifier = ScopeSpecIdentifier.Address(assetDefinition.scopeSpecAddress),
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
                expected = assetDefinition.copy(enabled = false),
                actual = newDefinition,
                message = "The definitions should be identical except for their enabled property",
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
            // I always mistype denom as demon so now I'm doing it on purpose because I CAN!
            val updatedVerifier = assetDefinition.verifiers.single().copy(onboardingCost = "88383838".toBigInteger(), onboardingDenom = "demon")
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
                    ownerAddress = AppResources.assetOnboardingAccount.bech32Address,
                    accessRoutes = AccessRoute("route", "name").wrapListAc(),
                ),
                signer = AppResources.assetOnboardingAccount.toAccountSigner()
            )
        }
        val asset = invoiceOnboardingService.onboardTestAsset(ownerAccount = AppResources.assetOnboardingAccount)
        assertFails("Attempting to update access routes for an unrelated account should fail") {
            acClient.updateAccessRoutes(
                execute = UpdateAccessRoutesExecute(
                    identifier = AssetIdentifier.AssetUuid(asset.assetUuid),
                    ownerAddress = AppResources.assetManagerAccount.bech32Address,
                    accessRoutes = AccessRoute("route", "name").wrapListAc()
                ),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        val scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(asset.assetUuid)
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
                ownerAddress = AppResources.assetOnboardingAccount.bech32Address,
                accessRoutes = newAccessRoutes,
            ),
            signer = AppResources.assetOnboardingAccount.toAccountSigner(),
        )
        acClient.queryAssetScopeAttributeByAssetUuid(asset.assetUuid)
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
                ownerAddress = AppResources.assetOnboardingAccount.bech32Address,
                accessRoutes = originalAccessRoutes,
            ),
            signer = AppResources.contractAdminAccount.toAccountSigner(),
        )
        acClient.queryAssetScopeAttributeByAssetUuid(asset.assetUuid)
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
    fun `test bindContractAlias`() {
        val bindAnAlias: (aliasName: String, account: ProvenanceAccountDetail) -> Unit = { aliasName, account ->
            acClient.bindContractAlias(
                execute = BindContractAliasExecute(aliasName),
                signer = account.toAccountSigner(),
            )
        }
        assertFails("Binding a contract alias with a non-admin account should fail") {
            bindAnAlias("testalias.pb", AppResources.assetOnboardingAccount)
        }
        assertFails("Binding an empty contract alias should fail") {
            bindAnAlias("", AppResources.contractAdminAccount)
        }
        bindAnAlias("goodalias.pb", AppResources.contractAdminAccount)
        assertEquals(
            expected = acClient.queryContractAddress(),
            actual = pbClient.nameClient.resolveAddressForName("goodalias.pb"),
            message = "The contract alias should be bound to the contract's address correctly",
        )
    }

    @Test
    fun `test deleteAssetDefinition`() {
        assertFails("Attempting to delete an asset definition that does not exist should fail") {
            acClient.deleteAssetDefinition(
                execute = DeleteAssetDefinitionExecute(AssetQualifier.AssetType("faketype")),
                signer = AppResources.contractAdminAccount.toAccountSigner(),
            )
        }
        acClient.addAssetDefinition(
            execute = AddAssetDefinitionExecute(
                assetType = "deleteme",
                scopeSpecIdentifier = ScopeSpecIdentifier.Uuid(UUID.randomUUID()),
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
                execute = DeleteAssetDefinitionExecute(AssetQualifier.AssetType("deleteme")),
                signer = AppResources.assetOnboardingAccount.toAccountSigner(),
            )
        }
        acClient.deleteAssetDefinition(
            execute = DeleteAssetDefinitionExecute(AssetQualifier.AssetType("deleteme")),
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
            // The verifier detail is not stored on chain to save space
            expected = scopeAttribute.copy(latestVerifierDetail = null),
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
                scopeSpecIdentifier = ScopeSpecIdentifier.Uuid(UUID.randomUUID()),
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
        testFunction.invoke(assetDefinition)
        acClient.deleteAssetDefinition(
            execute = DeleteAssetDefinitionExecute(qualifier = AssetQualifier.AssetType(assetType)),
            signer = AppResources.contractAdminAccount.toAccountSigner(),
        )
        assertNull(
            actual = acClient.queryAssetDefinitionByAssetTypeOrNull(assetType),
            message = "Expected the asset definition to be removed after deleting it.  Bad test behavior might occur after this failure",
        )
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
