package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.client.protobuf.extensions.time.toOffsetDateTimeOrNull
import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.TxEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import tech.figure.block.api.client.BlockAPIClient
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.config.RecoveryStatus
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import kotlin.time.Duration.Companion.milliseconds
import tech.figure.classification.asset.verifier.config.FailedBlockRetry

class BlockApiEventStreamProvider(
    private val blockApiClient: BlockAPIClient,
    private val coroutineScope: CoroutineScope,
    private val retry: FailedBlockRetry? = null
) : EventStreamProvider {

    companion object {
        const val DEFAULT_BLOCK_DELAY_MS: Double = 4000.0
    }

    override suspend fun currentHeight(): Long =
        blockApiClient.status().currentHeight

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ): RecoveryStatus {

        var current = currentHeight()
        var from = height ?: 1

        if (from > current) throw IllegalArgumentException("Cannot fetch block greater than the current height! Requested: $height, current: $current")

        try {
            while (coroutineScope.isActive) {
                (from..current).forEach { blockHeight ->
                    if (from >= current) return@forEach
                    process(blockHeight, onBlock, onEvent, onError, onCompletion)
                }

                // Once we've met the current block, no need to keep spinning. Wait here for 4 seconds and process again.
                delay(DEFAULT_BLOCK_DELAY_MS.milliseconds)
                from = current + 1
                current = currentHeight()
            }
        } catch (ex: Exception) {
            onError(ex)
            return RecoveryStatus.IRRECOVERABLE
        }

        return RecoveryStatus.RECOVERABLE
    }

    private suspend fun process(
        height: Long,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ) {
        runCatching {
            retry?.tryAction {
                getBlock(height, onBlock, onEvent)
            } ?: getBlock(height, onBlock, onEvent)
        }.onFailure { error ->
            onError(error)
        }
            .onSuccess {
                onCompletion(null)
            }
    }

    private suspend fun getBlock(
        height: Long,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
    ) {
        blockApiClient.getBlockByHeight(height, BlockServiceOuterClass.PREFER.TX).also {

            onBlock(it.block.height)

            toAssetClassificationEvent(it).forEach { classificationEvent ->
                onEvent(classificationEvent)
            }
        }
    }

    private fun toAssetClassificationEvent(data: BlockServiceOuterClass.BlockResult): List<AssetClassificationEvent> =
        data.transactions.transactions.transactionList.flatMap { tx ->
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
