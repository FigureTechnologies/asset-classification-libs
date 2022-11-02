package tech.figure.classification.asset.verifier.util.eventstream.providers

import io.provenance.eventstream.decoder.moshiDecoderAdapter
import io.provenance.eventstream.net.defaultOkHttpClient
import io.provenance.eventstream.net.okHttpNetAdapter
import io.provenance.eventstream.stream.clients.BlockData
import io.provenance.eventstream.stream.models.extensions.dateTime
import io.provenance.eventstream.stream.models.extensions.txData
import io.provenance.eventstream.stream.models.extensions.txEvents
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import tech.figure.classification.asset.verifier.provenance.WASM_EVENT_TYPE
import tech.figure.classification.asset.verifier.util.eventstream.verifierBlockDataFlow
import java.net.URI

class DefaultEventStreamProvider(
    eventStreamNode: URI = URI("ws://localhost:26657"),
    httpClient: OkHttpClient = defaultOkHttpClient()
) : EventStreamProvider {

    private val netAdapter = okHttpNetAdapter(
        node = eventStreamNode.toString(),
        okHttpClient = httpClient,
    )

    private val decoderAdapter = moshiDecoderAdapter()

    override suspend fun currentHeight(): Long? =
        netAdapter.rpcAdapter.getCurrentHeight()

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onError: suspend (throwable: Throwable, recoverable: Boolean) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ) {
        verifierBlockDataFlow(netAdapter, decoderAdapter, from = height)
            .catch { e -> onError(e, true) }
            .onCompletion { t -> onCompletion(t) }
            .onEach { block ->
                onBlock(block.height)
            }
            // Map all captured block data to AssetClassificationEvents, which will remove all non-wasm events
            // encountered
            .map { toAssetClassificationEvent(it) }
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
            onError(e, true)
            // Escape the loop entirely if the adapter fails to shut down - there should never be two adapters running
            // in tandem via this client
            return
        }
    }

    private fun toAssetClassificationEvent(data: BlockData) =
        data.blockResult
            // Use the event stream library's excellent extension functions to grab the needed TxEvent from
            // the block result, using the same strategy that their EventStream object does
            .txEvents(data.block.header?.dateTime()) { index -> data.block.txData(index) }
            // Only keep events of type WASM. All other event types are guaranteed to be unrelated to the
            // Asset Classification smart contract. This check can happen prior to any other parsing of data inside
            // the TxEvent, which will be a minor speed increase to downstream processing
            .filter { it.eventType == WASM_EVENT_TYPE }
            .map { event -> AssetClassificationEvent(event, inputValuesEncoded = true) }
}
