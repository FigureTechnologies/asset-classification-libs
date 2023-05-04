package tech.figure.classification.asset.verifier.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StreamRestartModeTest {
    @Test
    fun `test StreamRestartMode On immediate restart value is lower than min delay`() {
        // This test ensures that changes to these constants won't cause unexpected behavior
        assertTrue(
            actual = StreamRestartMode.On.MIN_RESTART_DELAY_MS > StreamRestartMode.On.RESTART_IMMEDIATELY,
            message = "Min delay [${StreamRestartMode.On.MIN_RESTART_DELAY_MS}] should be greater than immediate restart amount [${StreamRestartMode.On.RESTART_IMMEDIATELY}]"
        )
    }

    @Test
    fun `test StreamRestartMode On delay bounds`() {
        // The min value should be accepted and not throw an exception
        StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.MIN_RESTART_DELAY_MS)
        // The max value should be accepted and not throw an exception
        StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.MAX_RESTART_DELAY_MS)
        // Immediate restarts should be accepted even though they're less than the min restart delay MS
        StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.RESTART_IMMEDIATELY)
        assertFailsWith<IllegalStateException>("Values less than the immediate restart should not be accepted") {
            StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.RESTART_IMMEDIATELY - 0.00001)
        }
        assertFailsWith<IllegalStateException>("Values greater than the immediate restart should not be accepted") {
            StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.RESTART_IMMEDIATELY + 0.00001)
        }
        assertFailsWith<IllegalStateException>("Any value less than the min restart mode delay should be rejected") {
            StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.MIN_RESTART_DELAY_MS - 0.00001)
        }
        assertFailsWith<IllegalStateException>("Any value greater than the max restart mode delay should be rejected") {
            StreamRestartMode.On(restartDelayMs = StreamRestartMode.On.MAX_RESTART_DELAY_MS + 0.00001)
        }
    }

    @Test
    fun `test calcDelay for immediate restarts`() {
        val restartModeWithBackoff = StreamRestartMode.On(
            restartDelayMs = StreamRestartMode.On.RESTART_IMMEDIATELY,
            useExponentialBackoff = true
        )
        repeat(10) { restartCount ->
            assertEquals(
                expected = Duration.ZERO,
                actual = restartModeWithBackoff.calcDelay(restartCount.toLong()),
                message = "WITH BACKOFF: Regardless of the restart count, immediate retries should never produce a delay, but it did for restart count [$restartCount]"
            )
        }
        val restartModeWithoutBackoff = StreamRestartMode.On(
            restartDelayMs = StreamRestartMode.On.RESTART_IMMEDIATELY,
            useExponentialBackoff = false
        )
        repeat(10) { restartCount ->
            assertEquals(
                expected = Duration.ZERO,
                actual = restartModeWithoutBackoff.calcDelay(restartCount.toLong()),
                message = "WITHOUT BACKOFF: Regardless of the restart count, immediate retries should never produce a delay, but it did for restart count [$restartCount]"
            )
        }
    }

    @Test
    fun `test calcDelay for specified restarts with backoff`() {
        // 1 Second restarts for easy-to-calculate test values
        val restartMode = StreamRestartMode.On(restartDelayMs = 1000.0, useExponentialBackoff = true)
        assertEquals(
            expected = 1L,
            actual = restartMode.calcDelay(0).inWholeSeconds,
            message = "The first (0th counter value) retry should use the configured value of 1 second multiply by 2^0 == 1"
        )
        assertEquals(
            expected = 2L,
            actual = restartMode.calcDelay(1).inWholeSeconds,
            message = "The second (1st counter value) retry should use the configured value of 1 second multiplied by 2^1 == 2"
        )
        assertEquals(
            expected = 4L,
            actual = restartMode.calcDelay(2).inWholeSeconds,
            message = "The third (2nd counter value) retry should use the configured value of 1 second multiplied by 2^2 == 4"
        )
        assertEquals(
            expected = 8L,
            actual = restartMode.calcDelay(3).inWholeSeconds,
            message = "The fourth (3rd counter value) retry should use the configured value of 1 second multiplied by 2^3 == 8"
        )
    }

    @Test
    fun `test calcDelay for specified restarts without backoff`() {
        val restartMode = StreamRestartMode.On(restartDelayMs = 1000.0, useExponentialBackoff = false)
        repeat(10) { restartCount ->
            assertEquals(
                expected = restartMode.restartDelayMs.milliseconds,
                actual = restartMode.calcDelay(restartCount.toLong()),
                message = "Regardless of restart count, the restartDelayMs should always be used as specified when useExponentialBackoff is disabled, but was different for restart count [$restartCount]"
            )
        }
    }
}
