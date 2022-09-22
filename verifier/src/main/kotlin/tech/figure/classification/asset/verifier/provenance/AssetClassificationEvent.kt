package tech.figure.classification.asset.verifier.provenance

import cosmos.tx.v1beta1.ServiceOuterClass.GetTxResponse
import io.provenance.eventstream.extensions.decodeBase64
import io.provenance.eventstream.stream.clients.BlockData
import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.TxEvent
import io.provenance.eventstream.stream.models.extensions.dateTime
import io.provenance.eventstream.stream.models.extensions.txData
import io.provenance.eventstream.stream.models.extensions.txEvents
import tech.figure.classification.asset.util.models.ProvenanceTxEvents
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * A collection of all values that can be emitted by the asset classification smart contract in an event, parsed
 * automatically from block data or manually-replicated events.
 *
 * @param sourceEvent The TxEvent that was used to build the inner values for this event.  Contains details about the
 * Provenance Blockchain values, as well as the inner attributes emitted by the asset classification smart contract.
 * @param inputValuesEncoded Denotes whether or not the key/value pairs in the blockchain attributes on the tx event
 * are base64 encoded.  Values produced through the event stream are encoded, but manually generated values by the
 * VerifierClient are not.
 */
class AssetClassificationEvent(
    val sourceEvent: TxEvent,
    private val inputValuesEncoded: Boolean,
) {
    val contractAddress: String? by lazy { this.getEventStringValue(ACContractKey.CONTRACT_ADDRESS) }
    val eventType: ACContractEvent? by lazy {
        this.getEventValue(ACContractKey.EVENT_TYPE) { ACContractEvent.forContractName(it) }
    }
    val assetType: String? by lazy { this.getEventStringValue(ACContractKey.ASSET_TYPE) }
    val scopeAddress: String? by lazy { this.getEventStringValue(ACContractKey.SCOPE_ADDRESS) }
    val verifierAddress: String? by lazy { this.getEventStringValue(ACContractKey.VERIFIER_ADDRESS) }
    val scopeOwnerAddress: String? by lazy { this.getEventStringValue(ACContractKey.SCOPE_OWNER_ADDRESS) }
    val newValue: String? by lazy { this.getEventStringValue(ACContractKey.NEW_VALUE) }
    val additionalMetadata: String? by lazy { this.getEventStringValue(ACContractKey.ADDITIONAL_METADATA) }

    companion object {
        private const val WASM_EVENT_TYPE = "wasm"

        fun fromBlockData(data: BlockData): List<AssetClassificationEvent> =
            data.blockResult
                // Use the event stream library's excellent extension functions to grab the needed TxEvent from
                // the block result, using the same strategy that their EventStream object does
                .txEvents(data.block.header?.dateTime()) { index -> data.block.txData(index) }
                // Only keep events of type WASM. All other event types are guaranteed to be unrelated to the
                // Asset Classification smart contract. This check can happen prior to any other parsing of data inside
                // the TxEvent, which will be a minor speed increase to downstream processing
                .filter { it.eventType == WASM_EVENT_TYPE }
                .map { event -> AssetClassificationEvent(event, inputValuesEncoded = true) }

        fun fromVerifierTxEvents(
            sourceTx: GetTxResponse,
            txEvents: List<ProvenanceTxEvents>
        ): List<AssetClassificationEvent> =
            txEvents.flatMap { it.events }
                .filter { it.type == WASM_EVENT_TYPE }
                .map { event ->
                    AssetClassificationEvent(
                        sourceEvent = TxEvent(
                            blockHeight = sourceTx.txResponse.height,
                            blockDateTime = try {
                                OffsetDateTime.parse(sourceTx.txResponse.timestamp, DateTimeFormatter.ISO_DATE_TIME)
                            } catch (e: Exception) {
                                null
                            },
                            txHash = sourceTx.txResponse.txhash,
                            eventType = event.type,
                            attributes = event.attributes.map { attribute ->
                                Event(
                                    key = attribute.key,
                                    value = attribute.value,
                                )
                            },
                            fee = sourceTx.tx.authInfo.fee.amountList.firstOrNull()?.amount?.toLongOrNull(),
                            denom = sourceTx.tx.authInfo.fee.amountList.firstOrNull()?.denom,
                            note = null,
                        ),
                        inputValuesEncoded = false,
                    )
                }
    }

    private val attributeMap: Map<String, TxAttribute> by lazy {
        sourceEvent
            .attributes
            .mapNotNull { event ->
                if (inputValuesEncoded) {
                    TxAttribute.fromEventOrNull(event)
                } else {
                    TxAttribute.fromUnEncodedEventOrNull(event)
                }
            }
            .associateBy { it.key }
    }

    private fun getEventStringValue(key: ACContractKey): String? = attributeMap[key.eventName]?.value

    private inline fun <reified T> getEventValue(key: ACContractKey, transform: (String) -> T): T? = try {
        getEventStringValue(key)?.let(transform)
    } catch (e: Exception) {
        null
    }

    private data class TxAttribute(val key: String, val value: String) {
        companion object {
            fun fromEventOrNull(event: Event): TxAttribute? =
                decodeValue(event.key)?.let { key ->
                    decodeValue(event.value)?.let { value ->
                        TxAttribute(key.lowercase(), value)
                    }
                }

            fun fromUnEncodedEventOrNull(event: Event): TxAttribute? =
                event.key?.lowercase()?.let { key ->
                    event.value?.let { value ->
                        TxAttribute(key, value)
                    }
                }

            private fun decodeValue(value: String? = null): String? = try {
                value?.decodeBase64()
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun toString(): String = "AssetClassificationEvent[" +
        "eventType=$eventType, " +
        "assetType=$assetType, " +
        "scopeAddress=$scopeAddress, " +
        "verifierAddress=$verifierAddress, " +
        "scopeOwnerAddress=$scopeOwnerAddress, " +
        "newValue=$newValue]"
}
