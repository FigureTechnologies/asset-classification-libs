package tech.figure.classification.asset.verifier.config

import io.provenance.eventstream.utils.backoff
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Denotes whether or not the stream should be restarted when it fails internally.
 */
sealed interface StreamRestartMode {
    /**
     * Restart the stream after the specified delay in milliseconds.  This value cannot exceed one minute's worth of
     * milliseconds to ensure restart backoffs do not halt the stream for an excessive amount of time.  This value will
     * be used as the base restart delay, and subsequent restarts will exponentially increase the delay value based on
     * event-stream's backoff Duration calculation function.
     *
     * @param restartDelayMs The amount of ms to delay restarting the block flow after a failure occurs or the stream
     * completes.  To restart without waiting, use the RESTART_IMMEDIATELY companion object value for this param.
     * @param useExponentialBackoff If true, event-stream utils provides a backoff function that increases the delay
     * exponentially (2 to the power of X, where X is the amount of times the flow has restarted) that is used for this
     * purpose.  If this value is false, the restartDelayMs value will always be used for each restart.  If immediate
     * restarts are requested, this value is ignored.
     */
    class On(
        val restartDelayMs: Double = DEFAULT_RESTART_DELAY_MS,
        val useExponentialBackoff: Boolean = true,
    ) : StreamRestartMode {
        companion object {
            const val DEFAULT_RESTART_DELAY_MS: Double = 2000.0
            const val MIN_RESTART_DELAY_MS: Double = 1000.0
            const val MAX_RESTART_DELAY_MS: Double = 3600000.0
            const val RESTART_IMMEDIATELY: Double = 0.0
        }

        init {
            check(restartDelayMs == RESTART_IMMEDIATELY || restartDelayMs in MIN_RESTART_DELAY_MS..MAX_RESTART_DELAY_MS) { "Restart delay ms cannot be out of bounds: ${MIN_RESTART_DELAY_MS}ms - ${MAX_RESTART_DELAY_MS}ms" }
        }

        fun calcDelay(restartCount: Long): Duration = if (restartDelayMs != RESTART_IMMEDIATELY) {
            if (useExponentialBackoff) {
                // Use event-stream's nice backoff calculator to determine how many MS to wait until the next restart
                // based on the amount of restarts that have occurred
                backoff(
                    attempt = restartCount,
                    base = restartDelayMs,
                    jitter = false,
                )
            } else {
                // Without exponential backoff, just parse the delay ms into a Kotlin Duration as milliseconds
                restartDelayMs.milliseconds
            }
        } else {
            // If the delay ms value is set to immediate restarts, just set a duration of zero to make the delay invocation
            // be skipped
            Duration.ZERO
        }
    }

    /**
     * Never let the stream restart on its own.  Manual invocations of startVerifying() must be done to restart the
     * stream if a failure occurs.
     */
    object Off : StreamRestartMode
}
