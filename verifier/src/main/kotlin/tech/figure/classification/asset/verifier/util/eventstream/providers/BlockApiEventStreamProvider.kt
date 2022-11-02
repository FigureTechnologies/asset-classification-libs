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
    private val coroutineScope: CoroutineScope,
    private val batchSize: Long = 100L,
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

        val currentHeight = currentHeight()
        var startingHeight = height?.let {
            if (it > currentHeight) currentHeight else it
        } ?: getStartingBlockHeight(currentHeight)

        try {
            while (coroutineScope.isActive) {

                (startingHeight..(startingHeight + batchSize))
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

                startingHeight += batchSize
            }
        } catch (ex: Exception) {
            onError(ex)
            return RecoveryStatus.IRRECOVERABLE
        }

        return RecoveryStatus.RECOVERABLE
    }

    private fun getStartingBlockHeight(currentHeight: Long?): Long {
        return currentHeight?.let {
            it + 1
        } ?: 1
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
