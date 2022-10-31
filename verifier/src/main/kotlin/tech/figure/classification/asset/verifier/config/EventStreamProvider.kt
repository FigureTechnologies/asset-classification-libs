package tech.figure.classification.asset.verifier.config

import io.provenance.eventstream.stream.clients.BlockData
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent

interface EventStreamProvider {
    suspend fun currentHeight(): Long?
    suspend fun processBlockForHeight(
        height: Long? = null,
        onBlock: (suspend (block: BlockData) -> Unit),
        handleEvent: (suspend (event: AssetClassificationEvent) -> Unit),
        onError: (suspend (throwable: Throwable) -> Unit),
        onCompletion: (suspend (throwable: Throwable?) -> Unit),
        onNetAdapterShutdownFailure: (suspend (throwable: Throwable) -> Unit)
    )
}
