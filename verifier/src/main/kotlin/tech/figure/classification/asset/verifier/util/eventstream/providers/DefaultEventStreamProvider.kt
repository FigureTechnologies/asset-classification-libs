package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.eventstream.decoder.moshiDecoderAdapter
import io.provenance.eventstream.net.defaultOkHttpClient
import io.provenance.eventstream.net.okHttpNetAdapter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import tech.figure.classification.asset.verifier.util.eventstream.verifierBlockDataFlow
import tech.figure.classification.asset.verifier.config.EventStreamBlockData
import java.net.URI

class DefaultEventStreamProvider(
    eventStreamNode: URI = URI("ws://localhost:26657"),
    httpClient: OkHttpClient = defaultOkHttpClient()
) : EventStreamProvider<EventStreamBlockData> {

    private val netAdapter = okHttpNetAdapter(
        node = eventStreamNode.toString(),
        okHttpClient = httpClient,
    )

    private val decoderAdapter = moshiDecoderAdapter()

    override suspend fun currentHeight(): Long? =
        netAdapter.rpcAdapter.getCurrentHeight()

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (block: EventStreamBlockData) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ) {
        verifierBlockDataFlow(netAdapter, decoderAdapter, from = height)
            .catch { e -> onError(e) }
            .onCompletion { t -> onCompletion(t) }
            .onEach { block ->
                onBlock(EventStreamBlockData(block))
            }
            // Map all captured block data to AssetClassificationEvents, which will remove all non-wasm events
            // encountered
            .map(AssetClassificationEvent::fromBlockData)
            .collect { events ->
                events.forEach { event -> onEvent(event) }
            }
        // The event stream flow should execute infinitely unless some error occurs, so this line will only be reached
        // on connection failures or other problems.
        try {
            // Attempt to shut down the net adapter before restarting or exiting the stream
            netAdapter.shutdown()
        } catch (e: Exception) {
            // Emit the exception encountered on net adapter shutdown and exit the stream entirely
            onError(e)
            // Escape the loop entirely if the adapter fails to shut down - there should never be two adapters running
            // in tandem via this client
            return
        }
    }
}
