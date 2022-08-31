package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import cosmos.base.v1beta1.CoinOuterClass.Coin

@JsonNaming(SnakeCaseStrategy::class)
data class FeePayment(
    val amount: Coin,
    val name: String,
    val recipient: String,
)
