package tech.figure.classification.asset.verifier.config

import io.provenance.eventstream.stream.clients.BlockData
import tech.figure.block.api.proto.BlockServiceOuterClass
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent

interface EventStreamProvider<out T> {
    suspend fun currentHeight(): Long?
    suspend fun startProcessingFromHeight(
        height: Long? = null,
        onBlock: (suspend (block: T) -> Unit),
        onEvent: (suspend (event: AssetClassificationEvent) -> Unit),
        onError: (suspend (throwable: Throwable) -> Unit),
        onCompletion: (suspend (throwable: Throwable?) -> Unit)
    )
}

interface EventStreamProviderBlockData
data class EventStreamBlockData(val blockData: BlockData) : EventStreamProviderBlockData
data class BlockApiBlockData(val blockData: BlockServiceOuterClass.BlockResult) : EventStreamProviderBlockData
