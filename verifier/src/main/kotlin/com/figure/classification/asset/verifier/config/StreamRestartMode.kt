package com.figure.classification.asset.verifier.config

/**
 * Denotes whether or not the stream should be restarted when it fails internally.
 */
sealed interface StreamRestartMode {
    /**
     * Restart the stream after the specified delay in milliseconds.
     */
    class On(val restartDelayMs: Long = 5000L) : StreamRestartMode

    /**
     * Never let the stream restart on its own.  Manual invocations of startVerifying() must be done to restart the
     * stream if a failure occurs.
     */
    object Off : StreamRestartMode
}
