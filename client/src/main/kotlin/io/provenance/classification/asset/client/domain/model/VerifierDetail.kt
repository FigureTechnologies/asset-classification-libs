package io.provenance.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.extensions.toUint128CompatibleStringAc
import java.math.BigDecimal

/**
 * A configuration for a verifier's interactions with the Asset Classification smart contract.
 *
 * @param address The bech32 address of the verifier. A verifier application should have full access to this address in
 * order to execute the Asset Classification smart contract with this address as the signer.
 * @param onboardingCost A numeric representation of a specified coin amount to be taken during onboarding.  This value
 * will be distributed to the verifier and its fee destinations based on those configurations.
 * @param onboardingDenom The denomination of coin required for onboarding.  This value is used in tandem with
 * onboarding cost to determine a coin required.
 * @param feeDestinations A collection of addresses and fee distribution amounts that dictates how the fee amount is
 * distributed to other addresses than the verifier.  The amounts of all destinations should never sum to a value
 * greater than the onboarding cost.
 * @param entityDetail An optional set of fields defining the validator in a human-readable way.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class VerifierDetail(
    val address: String,
    val onboardingCost: String,
    val onboardingDenom: String,
    val feeDestinations: List<FeeDestination> = emptyList(),
    val entityDetail: EntityDetail? = null,
) {
    companion object {
        fun new(
            address: String,
            onboardingCost: BigDecimal,
            onboardingDenom: String,
            feeDestinations: List<FeeDestination> = emptyList(),
            entityDetail: EntityDetail? = null,
        ): VerifierDetail = VerifierDetail(
            address = address,
            // The cost must not have any decimal places - remove them before setting the value. This represents a coin amount
            onboardingCost = onboardingCost.toUint128CompatibleStringAc(),
            onboardingDenom = onboardingDenom,
            feeDestinations = feeDestinations,
            entityDetail = entityDetail,
        )
    }
}
