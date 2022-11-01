package tech.figure.classification.asset.verifier.util.eventstream.providers

import tech.figure.block.api.client.BlockAPIClient
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.config.BlockApiBlockData
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent

class BlockApiEventStreamProvider(
    private val blockApiClient: BlockAPIClient,
    private val batchSize: Long = 100L,
) : EventStreamProvider<BlockApiBlockData> {

    override suspend fun currentHeight(): Long =
        blockApiClient.status().currentHeight

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (block: BlockApiBlockData) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ) {
        val currentHeight = currentHeight()
        val startingHeight = height?.let {
            if (it > currentHeight) currentHeight else it
        } ?: getStartingBlockHeight(currentHeight)

        (startingHeight..(startingHeight + batchSize))
            .forEach { blockHeight ->
                runCatching {
                    blockApiClient.getBlockByHeight(
                        blockHeight,
                        BlockServiceOuterClass.PREFER.TX
                    ).also {
                        onBlock(BlockApiBlockData(it))

                        AssetClassificationEvent.fromBlockData(it).forEach { classificationEvent ->
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

        startProcessingFromHeight(startingHeight + batchSize, onBlock, onEvent, onError, onCompletion)
    }

    private fun getStartingBlockHeight(currentHeight: Long?): Long {
        return currentHeight?.let {
            it + 1
        } ?: 1
    }
}
