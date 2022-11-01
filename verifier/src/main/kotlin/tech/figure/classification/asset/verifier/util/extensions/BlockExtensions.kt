package tech.figure.classification.asset.verifier.util.extensions

import com.google.protobuf.Timestamp
import io.provenance.eventstream.stream.clients.BlockData
import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.extensions.dateTime
import io.provenance.eventstream.stream.models.extensions.txData
import io.provenance.eventstream.stream.models.extensions.txEvents
import tech.figure.block.api.proto.BlockServiceOuterClass
import io.provenance.eventstream.stream.models.TxEvent as ProvenanceTxEvent
import tech.figure.block.api.proto.BlockOuterClass.TxEvent as BlockApiTxEvent
import io.provenance.client.protobuf.extensions.time.toOffsetDateTimeOrNull
import io.provenance.eventstream.extensions.decodeBase64
import io.provenance.eventstream.extensions.stripQuotes
import tech.figure.classification.asset.verifier.data.EventDto

fun BlockData.extractEvents(): List<EventDto> = this.blockResult.txEvents(this.block.dateTime()) { i ->
    this.block.txData(i)
}.map { it.toEventDto() }


fun BlockServiceOuterClass.BlockResult.extractEvents(): List<EventDto> =
    this.transactions.transactions.transactionList.flatMap { tx ->
        tx.eventsList.map { it.toEventDto(tx.blockHeight, this.block.time) }
    }

fun ProvenanceTxEvent.toEventDto() = EventDto(
    blockHeight = this.blockHeight,
    blockDateTime = this.blockDateTime,
    txHash = this.txHash,
    eventType = this.eventType,
    attributes = this.attributes.decoded(),
    p8eEvent = this,
    inputValuesEncoded = true,
)

fun BlockApiTxEvent.toEventDto(blockHeight: Long, blockDateTime: Timestamp) = EventDto(
    blockHeight = blockHeight,
    blockDateTime = blockDateTime.toOffsetDateTimeOrNull(),
    txHash = this.txHash,
    eventType = this.eventType,
    attributes = this.attributesList.map {
        Event(
            key = it.key,
            value = it.value,
            index = it.index
        )
    },
    p8eEvent = io.provenance.eventstream.stream.models.TxEvent(
        blockHeight = blockHeight,
        blockDateTime = null,
        txHash = this.txHash,
        eventType = this.eventType,
        attributes = this.attributesList.map {
            Event(
                key = it.key,
                value = it.value,
                index = it.index
            )
        },
        fee = null,
        denom = null,
        note = null,
    ),
    inputValuesEncoded = false,
)

fun extractAttributeValue(attributes: List<Event>, key: String): String? = attributes.firstOrNull { attr ->
    attr.key?.stripQuotes() == key
}?.value?.stripQuotes()

fun List<Event>.decoded() = this.map {
    Event(
        key = it.key?.decodeBase64()?.stripQuotes(),
        value = it.value?.decodeBase64()?.stripQuotes(),
        index = it.index
    )
}
