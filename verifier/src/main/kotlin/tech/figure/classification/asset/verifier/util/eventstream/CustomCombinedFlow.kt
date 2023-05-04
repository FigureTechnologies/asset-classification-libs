package tech.figure.classification.asset.verifier.util.eventstream

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import tech.figure.eventstream.decoder.DecoderAdapter
import tech.figure.eventstream.net.NetAdapter
import tech.figure.eventstream.stream.clients.BlockData
import tech.figure.eventstream.stream.flows.historicalBlockDataFlow
import tech.figure.eventstream.stream.flows.wsBlockDataFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * This is nearly identical to the standard event-stream blockDataFlow function, but it omits baked-in retries.  This
 * is due to the fact that the event stream library will re-run previously-processed blocks when it dies unexpectedly
 * and does a retry, which is not the desired behavior of the verifier client.  This modified flow is combined with the
 * VerifierClient's built-in retries to enable restarts at the most recent failed block.
 */
fun verifierBlockDataFlow(
    netAdapter: NetAdapter,
    decoderAdapter: DecoderAdapter,
    from: Long? = null,
    to: Long? = null
): Flow<BlockData> = verifierCombinedFlow(
    getCurrentHeight = { netAdapter.rpcAdapter.getCurrentHeight() },
    from = from,
    to = to,
    getHeight = { it.height },
    historicalFlow = { f, t -> historicalBlockDataFlow(netAdapter, f, t) },
    liveFlow = { wsBlockDataFlow(netAdapter, decoderAdapter) }
)

/**
 * Stolen from event stream api.  Modified to prevent internally-dictated retries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> verifierCombinedFlow(
    getCurrentHeight: suspend () -> Long?,
    from: Long? = null,
    to: Long? = null,
    getHeight: (T) -> Long,
    historicalFlow: (from: Long, to: Long) -> Flow<T>,
    liveFlow: () -> Flow<T>
): Flow<T> = channelFlow {
    val channel = Channel<T>(capacity = 10_000) // buffer for: 10_000 * 6s block time / 60s/m / 60m/h == 16 2/3 hrs buffer time.
    val liveJob = async(coroutineContext) {
        liveFlow()
            .catch { channel.close(it) }
            .buffer(Channel.UNLIMITED)
            .collect { channel.send(it) }
    }

    val current = getCurrentHeight()!!

    // Determine if we need live data.
    // ie: if to is null, or more than current,
    val needLive = to == null || to > current

    // Determine if we need historical data.
    // ie: if from is null, or less than current, then we do.
    val needHist = from == null || from < current

    // Cancel live job and channel if unneeded.
    if (!needLive) {
        liveJob.cancel()
        channel.close()
    }

    // Process historical stream if needed.
    if (needHist) {
        val historyFrom = from ?: 1
        historicalFlow(historyFrom, current).collect {
            send(it)
            getHeight(it).also { h ->
                if (to != null && h >= to) {
                    close()
                }
            }
        }
    }

    // Live flow. Skip any dupe blocks.
    if (needLive) {
        // Continue receiving everything else live.
        // Drop anything between current head and the last fetched history record.
        val lastSeen = AtomicLong(0)
        channel.receiveAsFlow().collect { block ->
            send(block)
            getHeight(block).also { h ->
                lastSeen.set(h)
                if (to != null && h >= to) {
                    close()
                }
            }
        }

        while (!channel.isClosedForReceive) {
            val block = channel.receiveCatching().getOrThrow()
            val height = getHeight(block)

            // Skip if we have seen it already.
            if (height <= lastSeen.get()) {
                return@channelFlow
            }

            send(block)
            getHeight(block).also { h ->
                lastSeen.set(h)
                if (to != null && h >= to) {
                    close()
                }
            }
        }
    }
}.cancellable()
