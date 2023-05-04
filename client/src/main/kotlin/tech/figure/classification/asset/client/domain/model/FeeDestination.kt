package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import tech.figure.classification.asset.client.domain.serialization.CosmWasmBigIntegerToUintSerializer
import tech.figure.classification.asset.client.domain.serialization.CosmWasmUintToBigIntegerDeserializer
import java.math.BigInteger

/**
 * Defines a collector for fees for a validator.  All fee destinations should have fee percents totaling 100%, and the
 * contract runs validation to ensure that this is the case, so it can be assumed true in all cases.
 *
 * @param address The bech32 address of the recipient of fees.
 * @param feeAmount A value without decimal places that indicates how much coin this fee destination receives during
 * the onboarding process.
 * @param entityDetail A node of additional values that defines human-readable information about the fee destination.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class FeeDestination(
    val address: String,
    @JsonSerialize(using = CosmWasmBigIntegerToUintSerializer::class)
    @JsonDeserialize(using = CosmWasmUintToBigIntegerDeserializer::class)
    val feeAmount: BigInteger,
    val entityDetail: EntityDetail? = null
)
