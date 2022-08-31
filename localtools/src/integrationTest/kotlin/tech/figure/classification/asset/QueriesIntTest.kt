package tech.figure.classification.asset

import io.provenance.scope.util.MetadataAddress
import org.junit.jupiter.api.Test
import tech.figure.classification.asset.client.domain.execute.VerifyAssetExecute
import tech.figure.classification.asset.client.domain.model.AssetDefinition
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.client.domain.model.AssetScopeAttribute
import tech.figure.classification.asset.client.domain.model.FeePaymentDetail
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import tech.figure.spec.AssetSpecifications
import testconfiguration.IntTestBase
import testconfiguration.extensions.assertNotNullAc
import testconfiguration.models.TestAsset
import testconfiguration.util.AppResources
import java.math.BigInteger
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
        val assetDefinitions = acClient.queryAssetDefinitions()
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
        val asset = assetOnboardingService.storeAndOnboardTestAsset(
            assetType = "mortgage",
            ownerAccount = owner,
        )
        testAssetScopeAttributeValidity(
            asset = asset,
            scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuidOrNull(
                assetUuid = asset.assetUuid,
                assetType = asset.assetType,
            ),
            owner = AppResources.assetOnboardingAccount,
        )
        testAssetScopeAttributeValidity(
            asset = asset,
            scopeAttribute = acClient.queryAssetScopeAttributeByAssetUuid(
                assetUuid = asset.assetUuid,
                assetType = asset.assetType,
            ),
            owner = AppResources.assetOnboardingAccount,
        )
        assertNull(
            actual = acClient.queryAssetScopeAttributeByAssetUuidOrNull(UUID.randomUUID(), asset.assetType),
            message = "A null response should be returned when querying for an unknown scope attribute with queryAssetScopeAttributeByAssetUuidOrNull",
        )
        assertFails("An exception should be thrown when querying for an unknown scope attribute with queryAssetScopeAttributeByAssetUuid") {
            acClient.queryAssetScopeAttributeByAssetUuid(UUID.randomUUID(), asset.assetType)
        }
        assertNull(
            actual = acClient.queryAssetScopeAttributeByAssetUuidOrNull(asset.assetUuid, assetType = "unrelated"),
            message = "A null response should be returned when querying for an unknown asset type for an existing scope with queryAssetScopeAttributeByAssetUuidOrNull",
        )
        assertFails("An exception should be thrown when querying for an unknown asset type for an existing scope with queryAssetScopeAttributeByAssetUuid") {
            acClient.queryAssetScopeAttributeByAssetUuid(asset.assetUuid, assetType = "unrelated")
        }
    }

    @Test
    fun `test queryAssetScopeAttributeByScopeAddress`() {
        val owner = AppResources.assetOnboardingAccount
        val asset = assetOnboardingService.storeAndOnboardTestAsset(
            assetType = "heloc",
            ownerAccount = owner,
        )
        val scopeAddress = MetadataAddress.forScope(asset.assetUuid).toString()
        testAssetScopeAttributeValidity(
            asset = asset,
            scopeAttribute = acClient.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = scopeAddress,
                assetType = asset.assetType,
            ),
            owner = AppResources.assetOnboardingAccount,
        )
        testAssetScopeAttributeValidity(
            asset = asset,
            scopeAttribute = acClient.queryAssetScopeAttributeByScopeAddress(
                scopeAddress = scopeAddress,
                assetType = asset.assetType,
            ),
            owner = AppResources.assetOnboardingAccount,
        )
        assertNull(
            actual = acClient.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = "some scope address",
                assetType = asset.assetType,
            ),
            message = "A null response should be returned when querying for an unknown scope address with queryAssetScopeAttributeByScopeAddressOrNull",
        )
        assertFails("An exception should be thrown when querying for an unknown scope attribute with queryAssetScopeAttributeByScopeAddress") {
            acClient.queryAssetScopeAttributeByScopeAddress(
                scopeAddress = "some scope address",
                assetType = asset.assetType,
            )
        }
        assertNull(
            actual = acClient.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = scopeAddress,
                assetType = "unrelated",
            ),
            message = "A null response should be returned when querying for an unknown asset type for an existing scope with queryAssetScopeAttributeByScopeAddressOrNull",
        )
        assertFails("An exception should be thrown when querying for an unknown asset type for an existing scope with queryAssetScopeAttributeByScopeAddress") {
            acClient.queryAssetScopeAttributeByScopeAddress(
                scopeAddress = scopeAddress,
                assetType = "unrelated",
            )
        }
    }

    @Test
    fun `test queryAssetScopeAttributesByAssetUuid`() {
        assertNull(
            actual = acClient.queryAssetScopeAttributesByAssetUuidOrNull(UUID.randomUUID()),
            message = "Querying all asset scope attributes should produce null when using an unknown asset uuid with queryAssetScopeAttributesByAssetUuidOrNull",
        )
        assertFails("Querying all asset scope attributes should produce an exception when using an unknown asset uuid with queryAssetScopeAttributesByAssetUuid") {
            acClient.queryAssetScopeAttributesByAssetUuid(UUID.randomUUID())
        }
        val owner = AppResources.assetOnboardingAccount
        val asset = assetOnboardingService.createAsset(assetType = "heloc", ownerAccount = owner)
        // First, onboard the asset using its given type to create a heloc.asset attribute on the scope
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            ownerAccount = owner,
        )
        // Second, onboard the asset using the type "mortgage" to create a secondary mortgage.asset attribute on the scope.
        // Realistically, an asset would not likely be verified as both a heloc and a mortgage, but it's still an action that
        // can be taken to create two scope attributes on the same asset.  One or both of these, in a real scenario,
        // would get the status of AssetOnboardingStatus.DENIED for not including the correct payload.
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            assetType = "mortgage",
            ownerAccount = owner,
        )
        val testScopeAttributes: (attributes: List<AssetScopeAttribute>, functionName: String) -> Unit = { scopeAttributes, functionName ->
            assertEquals(
                expected = 2,
                actual = scopeAttributes.size,
                message = "Two scope attributes should be returned by $functionName",
            )
            scopeAttributes.forEach { scopeAttribute ->
                assertTrue(
                    actual = scopeAttribute.assetType in listOf("heloc", "mortgage"),
                    message = "$functionName: Expected the scope attribute's asset type to be one of the two asset types used, but got: ${scopeAttribute.assetType}",
                )
                testAssetScopeAttributeValidity(
                    asset = asset,
                    scopeAttribute = scopeAttribute,
                    owner = owner,
                    expectedAssetType = scopeAttribute.assetType,
                )
            }
        }
        testScopeAttributes(
            acClient.queryAssetScopeAttributesByAssetUuid(asset.assetUuid),
            "queryAssetScopeAttributesByAssetUuid",
        )
        testScopeAttributes(
            acClient.queryAssetScopeAttributesByAssetUuidOrNull(asset.assetUuid)
                .assertNotNullAc("Expected the queryAssetScopeAttributesByAssetUuidOrNull function to return a non-null result"),
            "queryAssetScopeAttributesByAssetUuidOrNull",
        )
    }

    @Test
    fun `test queryAssetScopeAttributesByScopeAddress`() {
        assertNull(
            actual = acClient.queryAssetScopeAttributesByScopeAddressOrNull("somescopeaddress"),
            message = "Querying all asset scope attributes should produce null when using an unknown asset uuid with queryAssetScopeAttributesByScopeAddressOrNull",
        )
        assertFails("Querying all asset scope attributes should produce an exception when using an unknown asset uuid with queryAssetScopeAttributesByScopeAddress") {
            acClient.queryAssetScopeAttributesByScopeAddress("somescopeaddress")
        }
        val owner = AppResources.assetOnboardingAccount
        val asset = assetOnboardingService.createAsset(assetType = "heloc", ownerAccount = owner)
        // First, onboard the asset using its given type to create a heloc.asset attribute on the scope
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            ownerAccount = owner,
        )
        // Second, onboard the asset using the type "mortgage" to create a secondary mortgage.asset attribute on the scope.
        // Realistically, an asset would not likely be verified as both a heloc and a mortgage, but it's still an action that
        // can be taken to create two scope attributes on the same asset.  One or both of these, in a real scenario,
        // would get the status of AssetOnboardingStatus.DENIED for not including the correct payload.
        assetOnboardingService.onboardTestAsset(
            asset = asset,
            assetType = "mortgage",
            ownerAccount = owner,
        )
        val testScopeAttributes: (attributes: List<AssetScopeAttribute>, functionName: String) -> Unit = { scopeAttributes, functionName ->
            assertEquals(
                expected = 2,
                actual = scopeAttributes.size,
                message = "Two scope attributes should be returned by $functionName",
            )
            scopeAttributes.forEach { scopeAttribute ->
                assertTrue(
                    actual = scopeAttribute.assetType in listOf("heloc", "mortgage"),
                    message = "$functionName: Expected the scope attribute's asset type to be one of the two asset types used, but got: ${scopeAttribute.assetType}",
                )
                testAssetScopeAttributeValidity(
                    asset = asset,
                    scopeAttribute = scopeAttribute,
                    owner = owner,
                    expectedAssetType = scopeAttribute.assetType,
                )
            }
        }
        val scopeAddress = MetadataAddress.forScope(asset.assetUuid).toString()
        testScopeAttributes(
            acClient.queryAssetScopeAttributesByScopeAddress(scopeAddress),
            "queryAssetScopeAttributesByScopeAddress",
        )
        testScopeAttributes(
            acClient.queryAssetScopeAttributesByScopeAddressOrNull(scopeAddress)
                .assertNotNullAc("Expected the queryAssetScopeAttributesByScopeAddressOrNull function to return a non-null result"),
            "queryAssetScopeAttributesByScopeAddressOrNull",
        )
    }

    @Test
    fun `test queryFeePaymentsByAssetUuid`() {
        assertNull(
            actual = acClient.queryFeePaymentsByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
            ),
            message = "Querying fee payments for an unknown asset should result in null for queryFeePaymentsByAssetUuidOrNull",
        )
        assertFails("Querying fee payments for an unknown asset should throw an exception for queryFeePaymentsByAssetUuid") {
            acClient.queryFeePaymentsByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
            )
        }
        val asset = assetOnboardingService.storeAndOnboardTestAsset()
        testFeePaymentDetailValidity(asset) {
            acClient.queryFeePaymentsByAssetUuid(
                assetUuid = asset.assetUuid,
                assetType = asset.assetType,
            )
        }
        acClient.verifyAsset(
            execute = VerifyAssetExecute(
                identifier = AssetIdentifier.AssetUuid(value = asset.assetUuid),
                assetType = asset.assetType,
                success = true,
                message = "We did it!",
            ),
            signer = AppResources.verifierAccount.toAccountSigner(),
        )
        assertNull(
            actual = acClient.queryFeePaymentsByAssetUuidOrNull(
                assetUuid = asset.assetUuid,
                assetType = asset.assetType,
            ),
            message = "queryFeePaymentsByAssetUuidOrNull should return null after verification completes because fee payments should be deleted",
        )
        assertFails("queryFeePaymentsByAssetUuid should throw an exception after verification completes because fee payments should be deleted") {
            acClient.queryFeePaymentsByAssetUuid(
                assetUuid = asset.assetUuid,
                assetType = asset.assetType,
            )
        }
    }

    @Test
    fun `test queryFeePaymentsByScopeAddress`() {
        assertNull(
            actual = acClient.queryFeePaymentsByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "heloc",
            ),
            message = "Querying fee payments for an unknown asset should result in null for queryFeePaymentsByScopeAddressOrNull",
        )
        assertFails("Querying fee payments for an unknown asset should throw an exception for queryFeePaymentsByScopeAddress") {
            acClient.queryFeePaymentsByScopeAddress(
                scopeAddress = "address",
                assetType = "heloc",
            )
        }
        val asset = assetOnboardingService.storeAndOnboardTestAsset()
        val scopeAddress = MetadataAddress.forScope(asset.assetUuid).toString()
        testFeePaymentDetailValidity(asset) {
            acClient.queryFeePaymentsByScopeAddress(
                scopeAddress = scopeAddress,
                assetType = asset.assetType,
            )
        }
        acClient.verifyAsset(
            execute = VerifyAssetExecute(
                identifier = AssetIdentifier.ScopeAddress(value = scopeAddress),
                assetType = asset.assetType,
                success = true,
                message = "We did it!",
            ),
            signer = AppResources.verifierAccount.toAccountSigner(),
        )
        assertNull(
            actual = acClient.queryFeePaymentsByScopeAddressOrNull(
                scopeAddress = scopeAddress,
                assetType = asset.assetType,
            ),
            message = "queryFeePaymentsByScopeAddressOrNull should return null after verification completes because fee payments should be deleted",
        )
        assertFails("queryFeePaymentsByScopeAddress should throw an exception after verification completes because fee payments should be deleted") {
            acClient.queryFeePaymentsByScopeAddress(
                scopeAddress = scopeAddress,
                assetType = asset.assetType,
            )
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
        expectedAssetType: String = asset.assetType,
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
            expected = expectedAssetType,
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

    private fun testFeePaymentDetailValidity(
        asset: TestAsset,
        getFeePaymentDetail: () -> FeePaymentDetail,
    ) {
        val feePayments = getFeePaymentDetail()
        assertEquals(
            expected = asset.assetType,
            actual = feePayments.assetType,
            message = "Expected the FeePaymentDetail to include the correct asset type",
        )
        assertEquals(
            expected = MetadataAddress.forScope(asset.assetUuid).toString(),
            actual = feePayments.scopeAddress,
            message = "Expected the FeePaymentDetail to include the correct scope address",
        )
        val verifierDetail = acClient.queryAssetDefinitionByAssetType(asset.assetType)
            .verifiers
            .singleOrNull { it.address == AppResources.verifierAccount.bech32Address }
            .assertNotNullAc("Expected the asset definition for asset type ${asset.assetType} to include the default verifier")
        val nonVerifierFeesPaid = feePayments
            .payments
            .filterNot { it.recipient == verifierDetail.address }
            .fold(BigInteger.ZERO) { nonVerifierFeeTotal, feePayment ->
                val feePaid = feePayment.amount.amount.toBigInteger()
                assertTrue(
                    actual = feePaid > BigInteger.ZERO,
                    message = "The fee paid for [${feePayment.name}] was zero or less ($feePaid), but fees generated should always be greater than zero",
                )
                assertEquals(
                    expected = verifierDetail.onboardingDenom,
                    actual = feePayment.amount.denom,
                    message = "All fee payments should use the verifier's denom, but [${feePayment.name}] did not",
                )
                nonVerifierFeeTotal + feePaid
            }
        val expectedVerifierFee = verifierDetail.onboardingCost
            .divide("2".toBigInteger())
            .minus(nonVerifierFeesPaid)
        assertTrue(
            actual = expectedVerifierFee >= BigInteger.ZERO,
            message = "Non verifier-targeted fees should always sum to a value less or equal to half the verifier detail's onboarding cost [${verifierDetail.onboardingCost}] divided by two, but was [$nonVerifierFeesPaid]",
        )
        if (expectedVerifierFee > BigInteger.ZERO) {
            val verifierFee = feePayments.payments.singleOrNull { it.recipient == verifierDetail.address }
                .assertNotNullAc("Expected a verifier fee to be produced")
            assertEquals(
                expected = expectedVerifierFee,
                actual = verifierFee.amount.amount.toBigInteger(),
                message = "The verifier fee produced was calculated incorrectly by the contract",
            )
            assertEquals(
                expected = verifierDetail.onboardingDenom,
                actual = verifierFee.amount.denom,
                message = "The verifier fee should use the verifier's onboarding denom for its amount denom",
            )
        } else {
            val matchingPayments = feePayments.payments.filter { it.recipient == verifierDetail.address }
            assertTrue(
                actual = matchingPayments.isEmpty(),
                message = "The verifier should not get a payment upon verification, but found matching fee payments:\n$matchingPayments",
            )
        }
    }
}
