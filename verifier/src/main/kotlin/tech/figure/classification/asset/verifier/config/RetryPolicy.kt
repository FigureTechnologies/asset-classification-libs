package tech.figure.classification.asset.verifier.config

import java.time.Duration

interface RetryPolicy {
    val times: Int
    val initialDelay: kotlin.time.Duration
    val factor: Double
    val maxDelay: kotlin.time.Duration
    suspend fun tryAction(action: (suspend () -> Unit))
}
