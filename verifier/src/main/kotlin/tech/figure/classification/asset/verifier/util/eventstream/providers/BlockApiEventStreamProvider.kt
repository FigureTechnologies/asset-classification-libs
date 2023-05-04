package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.client.protobuf.extensions.time.toOffsetDateTimeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import tech.figure.block.api.client.BlockAPIClient
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.config.RecoveryStatus
import tech.figure.classification.asset.verifier.config.RetryPolicy
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import tech.figure.eventstream.stream.models.Event
import tech.figure.eventstream.stream.models.TxEvent
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class BlockApiEventStreamProvider(
    private val blockApiClient: BlockAPIClient,
    private val coroutineScope: CoroutineScope,
    private val retry: RetryPolicy? = null
) : EventStreamProvider {

    companion object {
        const val DEFAULT_BLOCK_DELAY_MS: Double = 4000.0
    }

    override suspend fun currentHeight(): Long =
        currentHeightInternal()

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ): RecoveryStatus {
        val lastProcessed = AtomicLong(0)
        var current = currentHeightInternal { e -> onError(e) }
        var from = height ?: 1

        if (from > current) throw IllegalArgumentException("Cannot fetch block greater than the current height! Requested: $height, current: $current")

        try {
            while (coroutineScope.isActive) {
                (from..current).forEach { blockHeight ->
                    process(blockHeight, onBlock, onEvent, onError)
                    lastProcessed.set(blockHeight + 1)
                }

                // We've reached the current block, so fire the completion event
                onCompletion(null)

                // Once we've met the current block, no need to keep spinning. Wait here for 4 seconds and process again.
                delay(DEFAULT_BLOCK_DELAY_MS.milliseconds)
                from = lastProcessed.get()
                current = currentHeightInternal { e -> onError(e) }
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

    private suspend fun process(
        height: Long,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit
    ) {
        runCatching {
            retry?.tryAction {
                getBlock(height, onBlock, onEvent)
            } ?: getBlock(height, onBlock, onEvent)
        }.onFailure { error ->
            onError(error)
        }
    }

    private suspend fun getBlock(
        height: Long,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit
    ) {
        blockApiClient.getBlockByHeight(height, BlockServiceOuterClass.PREFER.TX_EVENTS).also {
            onBlock(it.block.height)

            toAssetClassificationEvent(it).forEach { classificationEvent ->
                onEvent(classificationEvent)
            }
        }
    }

    private fun toAssetClassificationEvent(data: BlockServiceOuterClass.BlockResult): List<AssetClassificationEvent> =
        data.block.transactionsList.flatMap { tx ->
            tx.eventsList.map { event ->
                AssetClassificationEvent(
                    TxEvent(
                        blockHeight = event.height,
                        txHash = event.txHash,
                        eventType = event.eventType,
                        attributes = event.attributesList.map { Event(it.key, it.value, it.index) },
                        blockDateTime = data.block.time.toOffsetDateTimeOrNull(),
                        fee = null,
                        denom = null,
                        note = null

                    ),
                    inputValuesEncoded = false
                )
            }
        }
}
