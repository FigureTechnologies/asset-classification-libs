package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import tech.figure.classification.asset.client.domain.serialization.CosmWasmBigIntegerToUintSerializer
import tech.figure.classification.asset.client.domain.serialization.CosmWasmUintToBigIntegerDeserializer
import java.math.BigInteger

/**
 * A generic representation of costs paid during onboarding.  This node is re-used throughout a VerifierDetail's values.
 *
 * @param cost The total amount charged to the onboarding requestor as a Provenance MsgFee during the onboard_asset
 * execution route.
 * @param feeDestinations A collection of addresses and fee distribution amounts that dictates how the fee amount is
 * distributed to other addresses than the verifier.  The amounts of all destinations should never sum to a value
 * greater than the onboarding cost.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class OnboardingCost(
    @JsonSerialize(using = CosmWasmBigIntegerToUintSerializer::class)
    @JsonDeserialize(using = CosmWasmUintToBigIntegerDeserializer::class)
    val cost: BigInteger,
    val feeDestinations: List<FeeDestination> = emptyList()
)
