package tech.figure.classification.asset.verifier.util.eventstream.retry

import kotlin.math.pow
import kotlinx.coroutines.delay
import tech.figure.classification.asset.verifier.config.FailedBlockRetry

class ExponentialBackoffFailedBlockRetry(
    override val times: Int = 5,
    override val initialDelay: Long = 100,
    override val factor: Double = 2.0,
    override val maxDelay: Long = 30000
) : FailedBlockRetry {
    override suspend fun tryAction(action: suspend () -> Unit) {
        var amount = initialDelay

        run retry@{
            repeat(times - 1) { times ->
                runCatching {
                    action()
                }
                    .onFailure {
                        delay(amount)
                        amount = factor.pow(times).toLong().coerceAtMost(maxDelay)
                    }
                    .onSuccess {
                        return@retry
                    }
            }
        }
    }
}
