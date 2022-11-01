package tech.figure.classification.asset.verifier.util.eventstream

import io.provenance.eventstream.decoder.moshiDecoderAdapter
import io.provenance.eventstream.net.defaultOkHttpClient
import io.provenance.eventstream.net.okHttpNetAdapter
import io.provenance.eventstream.stream.clients.BlockData
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import java.net.URI

class DefaultEventStreamProvider(
    eventStreamNode: URI?,
    httpClient: OkHttpClient?
) : EventStreamProvider {

    private val netAdapter = okHttpNetAdapter(
        node = eventStreamNode?.toString() ?: URI("ws://localhost:26657").toString(),
        okHttpClient = httpClient ?: defaultOkHttpClient(),
    )

    private val decoderAdapter = moshiDecoderAdapter()

    override suspend fun currentHeight(): Long? =
        netAdapter.rpcAdapter.getCurrentHeight()

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (block: BlockData) -> Unit,
        handleEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit,
        onNetAdapterShutdownFailure: suspend (throwable: Throwable) -> Unit
    ) {
        verifierBlockDataFlow(netAdapter, decoderAdapter, from = height)
            .catch { e -> onError(e) }
            .onCompletion { t -> onCompletion(t) }
            .onEach { block ->
                onBlock(block)
            }
            // Map all captured block data to AssetClassificationEvents, which will remove all non-wasm events
            // encountered
            .map(AssetClassificationEvent::fromBlockData)
            .collect { events ->
                events.forEach { event -> handleEvent(event) }
            }
        // The event stream flow should execute infinitely unless some error occurs, so this line will only be reached
        // on connection failures or other problems.
        try {
            // Attempt to shut down the net adapter before restarting or exiting the stream
            netAdapter.shutdown()
        } catch (e: Exception) {
            // Emit the exception encountered on net adapter shutdown and exit the stream entirely
            onNetAdapterShutdownFailure(e)
            // Escape the loop entirely if the adapter fails to shut down - there should never be two adapters running
            // in tandem via this client
            return
        }
    }
}
