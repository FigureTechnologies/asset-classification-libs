package tech.figure.classification.asset.verifier.config

import kotlin.time.Duration

interface RetryPolicy {
    /**
     * The number of times the exponential backoff should be calculated and delayed.
     */
    val times: Int

    /**
     * The initial delay that occurs, regardless of the configured backoff.
     */
    val initialDelay: Duration

    /**
     * The base factor that should be used to calculate the backoff given the expression delay = factor.pow(number of failed attempts)
     */
    val factor: Double

    /**
     * The maximum duration that the policy will delay for
     */
    val maxDelay: Duration

    /**
     * Retries an action with the configured parameters, and returns the result of the action or the exception to the action if all retries have failed
     * @param action action to retry
     * @return The result of the action [T]
     */
    suspend fun <T> tryAction(action: (suspend () -> T)): T
}
