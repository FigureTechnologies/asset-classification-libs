package testconfiguration.assertions

import io.provenance.scope.util.MetadataAddress
import tech.figure.classification.asset.client.domain.model.FeePaymentDetail
import testconfiguration.IntTestBase.Companion.acClient
import testconfiguration.extensions.assertNotNullAc
import testconfiguration.models.TestAsset
import testconfiguration.util.AppResources
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts that the given asset has been onboarded to the asset classification smart contract and has the expected fee
 * payment details.  This assumes that verification has not yet been run for the asset, because after verification, the
 * fee payment detail should be deleted from the contract's storage.
 */
fun assertFeePaymentDetailValidity(
    asset: TestAsset,
    assetType: String = asset.assetType,
    isRetry: Boolean = false,
    isSubsequentClassification: Boolean = false,
    getFeePaymentDetail: () -> FeePaymentDetail = { acClient.queryFeePaymentsByAssetUuid(asset.assetUuid, assetType) },
) {
    val feePayments = getFeePaymentDetail()
    assertEquals(
        expected = MetadataAddress.forScope(asset.assetUuid).toString(),
        actual = feePayments.scopeAddress,
        message = "Expected the FeePaymentDetail to include the correct scope address",
    )
    val verifierDetail = acClient.queryAssetDefinitionByAssetType(assetType)
        .verifiers
        .singleOrNull { it.address == AppResources.verifierAccount.bech32Address }
        .assertNotNullAc("Expected the asset definition for asset type $assetType to include the default verifier")
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
    val expectedOnboardingCost = verifierDetail.retryCost?.cost?.takeIf { isRetry }
        ?: verifierDetail.subsequentClassificationDetail?.cost?.cost?.takeIf { isSubsequentClassification }
        ?: verifierDetail.onboardingCost
    val expectedVerifierFee = expectedOnboardingCost.divide("2".toBigInteger()).minus(nonVerifierFeesPaid)
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
