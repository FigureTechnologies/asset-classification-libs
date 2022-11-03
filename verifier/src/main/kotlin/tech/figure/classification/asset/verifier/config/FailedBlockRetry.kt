package tech.figure.classification.asset.verifier.config

interface FailedBlockRetry {
    val times: Int
    val initialDelay: Long
    val factor: Double
    val maxDelay: Long
    suspend fun tryAction(action: (suspend () -> Unit))
}
