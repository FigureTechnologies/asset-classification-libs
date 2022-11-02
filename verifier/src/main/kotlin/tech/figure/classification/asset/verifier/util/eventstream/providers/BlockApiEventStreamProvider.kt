package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.client.protobuf.extensions.time.toOffsetDateTimeOrNull
import io.provenance.eventstream.stream.models.Event
import io.provenance.eventstream.stream.models.TxEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import tech.figure.block.api.client.BlockAPIClient
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.config.EventStreamProvider
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
    ) {

        val currentHeight = currentHeight()
        var startingHeight = height?.let {
            if (it > currentHeight) currentHeight else it
        } ?: getStartingBlockHeight(currentHeight)

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
    }

    private fun getStartingBlockHeight(currentHeight: Long?): Long {
        return currentHeight?.let {
            it + 1
        } ?: 1
    }

    override suspend fun <T> toAssetClassificationEvent(data: T): List<AssetClassificationEvent> =
        if (data is BlockServiceOuterClass.BlockResult) {
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
        } else throw IllegalArgumentException("Attempted to convert ${data!!::class.java.simpleName} to ${BlockServiceOuterClass.BlockResult::class.java.simpleName}")
}
