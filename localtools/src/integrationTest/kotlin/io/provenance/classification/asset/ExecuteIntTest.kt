package io.provenance.classification.asset

import com.fasterxml.jackson.module.kotlin.readValue
import io.provenance.attribute.v1.QueryAttributeRequest
import io.provenance.classification.asset.client.domain.execute.AddAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.DeleteAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.OnboardAssetExecute
import io.provenance.classification.asset.client.domain.execute.ToggleAssetDefinitionExecute
import io.provenance.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute
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
        onboardingCost = "10",
        onboardingDenom = "nhash",
        feeDestinations = FeeDestination(
            address = AppResources.contractAdminAccount.bech32Address,
            feeAmount = "5",
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
