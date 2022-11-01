package tech.figure.classification.asset.verifier.config

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
