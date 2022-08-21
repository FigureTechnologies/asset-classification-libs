package tech.figure.classification.asset.verifier.testhelpers

import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.TxEvent
import io.provenance.eventstream.stream.models.extensions.toHexString
import tech.figure.classification.asset.verifier.provenance.ACContractEvent
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import java.time.OffsetDateTime
import java.util.UUID

object MockTxEvent {
    fun builder(): MockTxEventBuilder = MockTxEventBuilder()

    class MockTxEventBuilder internal constructor() {
        private companion object {
            private const val DEFAULT_BLOCK_HEIGHT: Long = 1L
            private const val DEFAULT_EVENT_TYPE: String = "wasm"
            private const val DEFAULT_FEE_AMOUNT: Long = 0L
            private const val DEFAULT_DENOM: String = "nhash"
            private const val DEFAULT_NOTE: String = "MOCKED TX"
        }

        private var blockHeight: Long? = null
        private var blockDateTime: OffsetDateTime? = null
        private var txHash: String? = null
        private var eventType: String? = null
        private var attributes: MutableList<Event> = mutableListOf()
        private var fee: Long? = null
        private var denom: String? = null
        private var note: String? = null

        fun setBlockHeight(blockHeight: Long) = apply { this.blockHeight = blockHeight }
        fun setBlockDateTime(blockDateTime: OffsetDateTime) = apply { this.blockDateTime = blockDateTime }
        fun setTxHash(txHash: String) = apply { this.txHash = txHash }
        fun setEventType(eventType: String) = apply { this.eventType = eventType }
        fun setACEventType(eventType: ACContractEvent) = apply { this.eventType = eventType.contractName }
        fun addAttribute(attribute: Event) = apply { this.attributes.add(attribute) }
        fun addACAttribute(attribute: MockACAttribute) = apply { this.attributes.add(attribute.toAttribute()) }
        fun setFee(fee: Long) = apply { this.fee = fee }
        fun setDenom(denom: String) = apply { this.denom = denom }
        fun setNote(note: String) = apply { this.note = note }

        fun build(): TxEvent = TxEvent(
            blockHeight = blockHeight ?: DEFAULT_BLOCK_HEIGHT,
            blockDateTime = blockDateTime ?: OffsetDateTime.now(),
            txHash = txHash ?: UUID.randomUUID().toString().toByteArray().toHexString(),
            eventType = eventType ?: DEFAULT_EVENT_TYPE,
            attributes = attributes,
            fee = fee ?: DEFAULT_FEE_AMOUNT,
            denom = denom ?: DEFAULT_DENOM,
            note = note ?: DEFAULT_NOTE,
        )

        fun buildACEvent(): AssetClassificationEvent = AssetClassificationEvent(
            sourceEvent = build(),
            inputValuesEncoded = false,
        )
    }
}
