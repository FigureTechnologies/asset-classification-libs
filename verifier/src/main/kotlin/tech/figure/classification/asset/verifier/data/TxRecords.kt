package tech.figure.classification.asset.verifier.data

import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.TxEvent
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import java.time.OffsetDateTime

sealed class EventRecord(
    open val txHash: String,
    open val eventType: String,
)

data class ScopeEvent(
    override val txHash: String,
    override val eventType: String,
    val scopeAddr: String,
    val recordAddrs: MutableList<String>,
) : EventRecord(
    txHash = txHash,
    eventType = eventType,
)

data class TransferEvent(
    override val txHash: String,
    override val eventType: String,
    val denom: String,
    val amount: String,
    val admin: String,
    val fromAddress: String,
    val toAddress: String,
) : EventRecord(
    txHash = txHash,
    eventType = eventType,
)

data class ClassificationEvent(
    override val txHash: String,
    override val eventType: String,
    val assetClassificationEvent: AssetClassificationEvent,
) : EventRecord(
    txHash = txHash,
    eventType = eventType,
)

data class EventDto(
    val blockHeight: Long,
    val blockDateTime: OffsetDateTime?,
    override val txHash: String,
    override val eventType: String,
    val attributes: List<Event>,
    val p8eEvent: TxEvent,
    val inputValuesEncoded: Boolean,
) : EventRecord(
    txHash = txHash,
    eventType = eventType,
)
