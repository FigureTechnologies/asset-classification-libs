package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Defines a fee established from a verifier detail and its contained fee destinations.
 *
 * @param scopeAddress The bech32 address of the onboarded scope related to the fee.  This address is used as the unique
 * identifier for the fee, and to retrieve the associated asset scope attribute for finding the requestor's address to
 * which the fee is charged.
 * @param payments The breakdown of each fee charge.  This list will always contain at least o single charge, which will
 * be to send a payment to the verifier.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class FeePaymentDetail(
    val scopeAddress: String,
    val payments: List<FeePayment>
)
