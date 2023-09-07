package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.client.protobuf.extensions.time.toOffsetDateTimeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import tech.figure.block.api.client.BlockAPIClient
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.config.RecoveryStatus
import tech.figure.classification.asset.verifier.config.RetryPolicy
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import tech.figure.classification.asset.verifier.provenance.WASM_EVENT_TYPE
import tech.figure.eventstream.stream.models.Event
import tech.figure.eventstream.stream.models.TxEvent

class BlockApiEventStreamProvider(
    private val blockApiClient: BlockAPIClient,
    private val coroutineScope: CoroutineScope,
    private val retry: RetryPolicy? = null
) : EventStreamProvider {
    override suspend fun currentHeight(): Long =
        currentHeightInternal()

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onEventsProcessed: suspend (blockHeight: Long) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ): RecoveryStatus {
        try {
            blockApiClient.streamBlocks(
                start = height ?: 1,
                preference = BlockServiceOuterClass.PREFER.TX_EVENTS
            ).catch { e -> onError(e) }
                .onCompletion { t -> onCompletion(t) }
                .collect { block ->
                    val blockHeight = block.blockResult.block.height
                    onBlock(blockHeight)
                    // Map all captured block data to AssetClassificationEvents, which will remove all non-wasm events
                    // encountered
                    block.toAssetClassificationEvents().forEach { event -> onEvent(event) }
                    onEventsProcessed(blockHeight)
                }
        } catch (ex: Exception) {
            onError(ex)
        }

        return if (coroutineScope.isActive) {
            RecoveryStatus.RECOVERABLE
        } else {
            RecoveryStatus.IRRECOVERABLE
        }
    }

    private suspend fun currentHeightInternal(failureAction: (suspend (e: Throwable) -> Unit)? = null): Long =
        try {
            retry?.tryAction {
                blockApiClient.status().currentHeight
            } ?: run {
                blockApiClient.status().currentHeight
            }
        } catch (ex: Exception) {
            failureAction?.invoke(ex)
            throw IllegalArgumentException("Unable to get current height from block api!", ex)
        }

    private fun BlockServiceOuterClass.BlockStreamResult.toAssetClassificationEvents(): List<AssetClassificationEvent> {
        val blockDateTime by lazy { blockResult.block.time.toOffsetDateTimeOrNull() }
        return blockResult.block.transactionsList
            .flatMap { it.eventsList }
            // Only keep events of type WASM. All other event types are guaranteed to be unrelated to the
            // Asset Classification smart contract. This check can happen prior to any other parsing of data inside
            // the TxEvent, which will be a minor speed increase to downstream processing
            .filter { it.eventType == WASM_EVENT_TYPE }
            .map { event ->
                AssetClassificationEvent(
                    TxEvent(
                        blockHeight = event.height,
                        txHash = event.txHash,
                        eventType = event.eventType,
                        attributes = event.attributesList.map { Event(it.key, it.value, it.index) },
                        blockDateTime = blockDateTime,
                        fee = null,
                        denom = null,
                        note = null

                    ),
                    inputValuesEncoded = false
                )
            }
    }
}
