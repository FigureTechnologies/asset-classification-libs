package io.provenance.classification.asset

import io.provenance.classification.asset.client.domain.model.AssetDefinition
import io.provenance.classification.asset.client.domain.model.AssetScopeAttribute
import io.provenance.classification.asset.util.wallet.ProvenanceAccountDetail
import io.provenance.scope.util.MetadataAddress
import io.provenance.spec.AssetSpecification
import io.provenance.spec.AssetSpecifications
import io.provenance.spec.HELOCSpecification
import io.provenance.spec.MortgageSpecification
import org.junit.jupiter.api.Test
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
            expectedSpecification = HELOCSpecification,
        )
        testAssetDefinitionValidity(
            assetDefinition = acClient.queryAssetDefinitionByAssetType("heloc"),
            expectedAssetType = "heloc",
            expectedSpecification = HELOCSpecification,
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
    fun `test queryAssetDefinitionByScopeSpecAddress`() {
        val scopeSpecAddress = MetadataAddress.forScopeSpecification(MortgageSpecification.scopeSpecConfig.id).toString()
        testAssetDefinitionValidity(
            assetDefinition = acClient.queryAssetDefinitionByScopeSpecAddressOrNull(scopeSpecAddress),
            expectedAssetType = "mortgage",
            expectedSpecification = MortgageSpecification,
        )
        testAssetDefinitionValidity(
            assetDefinition = acClient.queryAssetDefinitionByScopeSpecAddress(scopeSpecAddress),
            expectedAssetType = "mortgage",
            expectedSpecification = MortgageSpecification,
        )
        val badScopeSpecAddress = MetadataAddress.forScopeSpecification(UUID.randomUUID()).toString()
        assertNull(
            actual = acClient.queryAssetDefinitionByScopeSpecAddressOrNull(badScopeSpecAddress),
            message = "Expected a missing scope spec address to produce null for queryAssetDefinitionByScopeSpecAddressOrNull",
        )
        assertFails("Expected a missing scope spec address to throw an exception for queryAssetDefinitionByScopeSpecAddress") {
            acClient.queryAssetScopeAttributeByScopeAddress(badScopeSpecAddress)
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
            val targetScopeSpecAddress = assetSpecification.getScopeSpecAddress()
            val assetDefinition = assetDefinitions.singleOrNull { it.scopeSpecAddress == targetScopeSpecAddress }
            assertNotNull(
                actual = assetDefinition,
                message = "Expected an asset definition of type [${assetSpecification.scopeSpecConfig.name}] to exist " +
                    "for address [$targetScopeSpecAddress] but none were found.  Available values: " +
                    assetDefinitions.joinToString(prefix = "[", separator = ", ", postfix = "]") {
                        "(Type: ${it.assetType} | Address: ${it.scopeSpecAddress})"
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

    private fun AssetSpecification.getScopeSpecAddress(): String = MetadataAddress
        .forScopeSpecification(scopeSpecConfig.id)
        .toString()

    private fun testAssetDefinitionValidity(
        assetDefinition: AssetDefinition?,
        expectedAssetType: String,
        expectedSpecification: AssetSpecification,
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
        assertEquals(
            expected = expectedSpecification.getScopeSpecAddress(),
            actual = assetDefinition.scopeSpecAddress,
            message = "Expected the asset definition to have the correct scope spec address",
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
