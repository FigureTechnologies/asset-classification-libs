package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import tech.figure.classification.asset.client.domain.serialization.CosmWasmBigIntegerToUintSerializer
import tech.figure.classification.asset.client.domain.serialization.CosmWasmUintToBigIntegerDeserializer
import java.math.BigInteger

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
 * @param retryCost Defines the cost to use in place of the root onboarding cost and fee destination when retrying
 * classification for a failed verification.  If not present, the original values used for the first verification will
 * be used.
 * @param subsequentClassificationDetail An optional set of fields that define behaviors when classification is being
 * run for an asset that is already classified as a different type.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class VerifierDetail(
    val address: String,
    @JsonSerialize(using = CosmWasmBigIntegerToUintSerializer::class)
    @JsonDeserialize(using = CosmWasmUintToBigIntegerDeserializer::class)
    val onboardingCost: BigInteger,
    val onboardingDenom: String,
    val feeDestinations: List<FeeDestination> = emptyList(),
    val entityDetail: EntityDetail? = null,
    val retryCost: OnboardingCost? = null,
    val subsequentClassificationDetail: SubsequentClassificationDetail? = null,
)
