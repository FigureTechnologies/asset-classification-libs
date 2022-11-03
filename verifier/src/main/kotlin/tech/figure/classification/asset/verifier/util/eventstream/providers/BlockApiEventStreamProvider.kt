package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.client.protobuf.extensions.time.toOffsetDateTimeOrNull
import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.TxEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import tech.figure.block.api.client.BlockAPIClient
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.config.RecoveryStatus
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent

class BlockApiEventStreamProvider(
    private val blockApiClient: BlockAPIClient,
    private val coroutineScope: CoroutineScope
) : EventStreamProvider {

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
                if (from < current) {
                    (from..current)
                        .forEach { blockHeight ->
                            runCatching {
                                blockApiClient.getBlockByHeight(
                                    blockHeight,
                                    BlockServiceOuterClass.PREFER.TX
                                ).also {
                                    onBlock(it.block.height)

                                    toAssetClassificationEvent(it).forEach { classificationEvent ->
                                        onEvent(classificationEvent)
                                    }
                                }
                            }.onFailure { error ->
                                onError(error)
                            }
                                .onSuccess {
                                    onCompletion(null)
                                }
                        }
                }

                from = current
                current = currentHeight()
            }
        } catch (ex: Exception) {
            onError(ex)
            return RecoveryStatus.IRRECOVERABLE
        }

        return RecoveryStatus.RECOVERABLE
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
