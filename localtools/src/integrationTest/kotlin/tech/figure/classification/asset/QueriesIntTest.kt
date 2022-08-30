package tech.figure.classification.asset

import io.provenance.scope.util.MetadataAddress
import org.junit.jupiter.api.Test
import tech.figure.classification.asset.client.domain.model.AssetDefinition
import tech.figure.classification.asset.client.domain.model.AssetScopeAttribute
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import tech.figure.spec.AssetSpecifications
import testconfiguration.IntTestBase
import testconfiguration.models.TestAsset
import testconfiguration.util.AppResources
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueriesIntTest : IntTestBase() {
    @Test
    fun `test queryAssetDefinitionByAssetType`() {
        testAssetDefinitionValidity(
            assetDefinition = acClient.queryAssetDefinitionByAssetTypeOrNull("heloc"),
            expectedAssetType = "heloc",
        )
        testAssetDefinitionValidity(
            assetDefinition = acClient.queryAssetDefinitionByAssetType("heloc"),
            expectedAssetType = "heloc",
        )
        assertNull(
            actual = acClient.queryAssetDefinitionByAssetTypeOrNull("some type that doesn't exist"),
            message = "Expected a missing type to produce null for queryAssetDefinitionByAssetTypeOrNull",
        )
        assertFails("Expected a missing type to throw an exception for queryAssetDefinitionByAssetType") {
            acClient.queryAssetDefinitionByAssetType("some type that doesn't exist")
        }
    }

    @Test
    fun `test queryAssetDefinitions`() {
        val assetDefinitions = acClient.queryAssetDefinitions().assetDefinitions
        assertEquals(
            expected = AssetSpecifications.size,
            actual = assetDefinitions.size,
            message = "There should be one asset definition per asset specification",
        )
        AssetSpecifications.forEach { assetSpecification ->
            val targetAssetType = assetSpecification.scopeSpecConfig.name
            val assetDefinition = assetDefinitions.singleOrNull { it.assetType == targetAssetType }
            assertNotNull(
                actual = assetDefinition,
                message = "Expected an asset definition of type [${assetSpecification.scopeSpecConfig.name}] to exist " +
                    "but none were found.  Available values: " +
                    assetDefinitions.joinToString(prefix = "[", separator = ", ", postfix = "]") {
                        "(Type: ${it.assetType})"
                    }
            )
        }
    }

    @Test
    fun `test queryAssetScopeAttributeByAssetUuid`() {
        val owner = AppResources.assetOnboardingAccount
        val asset = invoiceOnboardingService.onboardTestAsset(assetType = "mortgage", ownerAccount = owner)
        testAssetScopeAttributeValidity(
            asset = asset,
            scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuidOrNull(asset.assetUuid),
            owner = AppResources.assetOnboardingAccount,
        )
        testAssetScopeAttributeValidity(
            asset = asset,
            scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(asset.assetUuid),
            owner = AppResources.assetOnboardingAccount,
        )
        assertNull(
            actual = acClient.queryAssetScopeAttributeByAssetUuidOrNull(UUID.randomUUID()),
            message = "A null response should be returned when querying for an unknown scope attribute with queryAssetScopeAttributeByAssetUuidOrNull",
        )
        assertFails("An exception should be thrown when querying for an unknown scope attribute with queryAssetScopeAttributeByAssetUuid") {
            acClient.queryAssetScopeAttributeByAssetUuid(UUID.randomUUID())
        }
    }

    @Test
    fun `test queryContractState`() {
        val state = acClient.queryContractState()
        assertEquals(
            expected = AppResources.contractAdminAccount.bech32Address,
            actual = state.admin,
            message = "The contract admin should be set the correct account address",
        )
        assertEquals(
            expected = "asset",
            actual = state.baseContractName,
            message = "The base contract name for the contract should be asset",
        )
    }

    @Test
    fun `test queryContractVersion`() {
        val versionInfo = acClient.queryContractVersion()
        assertEquals(
            expected = AppResources.CONTRACT_VERSION,
            actual = versionInfo.version,
            message = "The contract version should be the specified value for downloads",
        )
    }

    private fun testAssetDefinitionValidity(
        assetDefinition: AssetDefinition?,
        expectedAssetType: String,
    ) {
        assertNotNull(
            actual = assetDefinition,
            message = "Expected the asset definition to be non-null",
        )
        assertEquals(
            expected = expectedAssetType,
            actual = assetDefinition.assetType,
            message = "Expected the asset definition's asset type to be correct",
        )
        assertTrue(
            actual = assetDefinition.enabled,
            message = "Expected the asset definition to be enabled",
        )
        val verifier = assetDefinition.verifiers.singleOrNull()
        assertNotNull(
            actual = verifier,
            message = "A single verifier should be set for each asset definition",
        )
        assertEquals(
            expected = AppResources.verifierAccount.bech32Address,
            actual = verifier.address,
            message = "The default verifier address should be set on the verifier",
        )
    }

    private fun testAssetScopeAttributeValidity(
        asset: TestAsset,
        scopeAttribute: AssetScopeAttribute?,
        owner: ProvenanceAccountDetail,
    ) {
        assertNotNull(
            actual = scopeAttribute,
            message = "The provided attribute should not be null",
        )
        assertEquals(
            expected = asset.assetUuid,
            actual = scopeAttribute.assetUuid,
            message = "The scope attribute should include the correct asset uuid",
        )
        assertEquals(
            expected = asset.assetType,
            actual = scopeAttribute.assetType,
            message = "The scope attribute should include the correct asset type",
        )
        assertEquals(
            expected = MetadataAddress.forScope(asset.assetUuid).toString(),
            actual = scopeAttribute.scopeAddress,
            message = "The scope attribute should have the correct scope address",
        )
        assertEquals(
            expected = AppResources.verifierAccount.bech32Address,
            actual = scopeAttribute.verifierAddress,
            message = "The correct verifier address should be set on the scope attribute",
        )
        assertEquals(
            expected = owner.bech32Address,
            actual = scopeAttribute.requestorAddress,
            message = "The requestor address should be set to the asset owner address",
        )
    }
}
