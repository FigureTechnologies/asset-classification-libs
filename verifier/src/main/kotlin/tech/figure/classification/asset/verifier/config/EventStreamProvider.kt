package tech.figure.classification.asset.verifier.config

import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent

interface EventStreamProvider {
    suspend fun currentHeight(): Long?
    suspend fun startProcessingFromHeight(
        height: Long? = null,
        onBlock: (suspend (blockHeight: Long) -> Unit),
        onEvent: (suspend (event: AssetClassificationEvent) -> Unit),
        onError: (suspend (throwable: Throwable) -> Unit),
        onCompletion: (suspend (throwable: Throwable?) -> Unit)
    ): RecoveryStatus
}
