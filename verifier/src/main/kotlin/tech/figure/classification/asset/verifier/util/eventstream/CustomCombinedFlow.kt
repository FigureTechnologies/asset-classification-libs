package tech.figure.classification.asset.verifier.util.eventstream

import io.provenance.eventstream.decoder.DecoderAdapter
import io.provenance.eventstream.net.NetAdapter
import io.provenance.eventstream.stream.clients.BlockData
import io.provenance.eventstream.stream.flows.historicalBlockDataFlow
import io.provenance.eventstream.stream.flows.wsBlockDataFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicLong

fun verifierBlockDataFlow(
    netAdapter: NetAdapter,
    decoderAdapter: DecoderAdapter,
    from: Long? = null,
    to: Long? = null,
): Flow<BlockData> = verifierCombinedFlow(
    getCurrentHeight = { netAdapter.rpcAdapter.getCurrentHeight() },
    from = from,
    to = to,
    getHeight = { it.height },
    historicalFlow = { f, t -> historicalBlockDataFlow(netAdapter, f, t) },
    liveFlow = { wsBlockDataFlow(netAdapter, decoderAdapter) },
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
    liveFlow: () -> Flow<T>,
): Flow<T> = channelFlow<T> {
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
