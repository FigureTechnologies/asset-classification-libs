package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import cosmos.base.v1beta1.CoinOuterClass.Coin

/**
 * Defines an individual fee to be charged to an account during the asset verification process.
 *
 * @param amount The coin's amount will be equal to the amount for a fee_destination in the verifier detail, and
 * (onboarding_cost / 2) - fee_destination_total for the verifier itself if that amount is > 0.
 * @param name A name describing to the end user (requestor) the purpose and target of the fee.
 * @param recipient The bech32 address of the recipient of the fee, derived from various fields in the verifier detail.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class FeePayment(
    val amount: Coin,
    val name: String,
    val recipient: String,
)
