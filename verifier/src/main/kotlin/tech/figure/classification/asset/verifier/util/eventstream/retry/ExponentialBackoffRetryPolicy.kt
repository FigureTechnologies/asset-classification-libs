package tech.figure.classification.asset.verifier.util.eventstream.retry

import kotlinx.coroutines.delay
import tech.figure.classification.asset.verifier.config.RetryPolicy
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class ExponentialBackoffRetryPolicy(
    override val times: Int = 5,
    override val initialDelay: Duration = 1.seconds,
    override val factor: Double = 2.0,
    override val maxDelay: Duration = 20.seconds
) : RetryPolicy {
    override suspend fun tryAction(action: suspend () -> Unit) {
        var amount = initialDelay

        run retry@{
            repeat(times - 1) { times ->
                runCatching {
                    action()
                }
                    .onFailure {
                        delay(amount)
                        amount = factor.pow(times).toLong().coerceAtMost(maxDelay.toLong(DurationUnit.SECONDS)).seconds
                    }
                    .onSuccess {
                        return@retry
                    }
            }
        }
    }
}
