package tech.figure.classification.asset.verifier.util.eventstream.providers

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import okhttp3.OkHttpClient
import tech.figure.classification.asset.verifier.config.EventStreamProvider
import tech.figure.classification.asset.verifier.config.RecoveryStatus
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import tech.figure.classification.asset.verifier.provenance.WASM_EVENT_TYPE
import tech.figure.classification.asset.verifier.util.eventstream.verifierBlockDataFlow
import tech.figure.eventstream.decoder.moshiDecoderAdapter
import tech.figure.eventstream.net.defaultOkHttpClientBuilderFn
import tech.figure.eventstream.net.okHttpNetAdapter
import tech.figure.eventstream.stream.clients.BlockData
import tech.figure.eventstream.stream.models.dateTime
import tech.figure.eventstream.stream.models.txData
import tech.figure.eventstream.stream.models.txEvents
import java.net.URI

class DefaultEventStreamProvider(
    eventStreamNode: URI = URI("ws://localhost:26657"),
    httpClient: OkHttpClient = defaultOkHttpClientBuilderFn().run { OkHttpClient.Builder().this() }.build()
) : EventStreamProvider {

    private val netAdapter = okHttpNetAdapter(
        node = eventStreamNode.toString(),
        okHttpClient = httpClient
    )

    private val decoderAdapter = moshiDecoderAdapter()

    override suspend fun currentHeight(): Long? =
        netAdapter.rpcAdapter.getCurrentHeight()

    override suspend fun startProcessingFromHeight(
        height: Long?,
        onBlock: suspend (blockHeight: Long) -> Unit,
        onEvent: suspend (event: AssetClassificationEvent) -> Unit,
        onEventsProcessed: suspend (blockHeight: Long) -> Unit,
        onError: suspend (throwable: Throwable) -> Unit,
        onCompletion: suspend (throwable: Throwable?) -> Unit
    ): RecoveryStatus {
        verifierBlockDataFlow(netAdapter, decoderAdapter, from = height)
            .catch { e -> onError(e) }
            .onCompletion { t -> onCompletion(t) }
            .collect { block ->
                onBlock(block.height)
                // Map all captured block data to AssetClassificationEvents, which will remove all non-wasm events
                // encountered
                block.toAssetClassificationEvents().forEach { event -> onEvent(event) }
                onEventsProcessed(block.height)
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
            return RecoveryStatus.RECOVERABLE
        }

        return RecoveryStatus.RECOVERABLE
    }

    private fun BlockData.toAssetClassificationEvents(): List<AssetClassificationEvent> =
        blockResult
            // Use the event stream library's excellent extension functions to grab the needed TxEvent from
            // the block result, using the same strategy that their EventStream object does
            .txEvents(block.header?.dateTime()) { index -> block.txData(index) }
            // Only keep events of type WASM. All other event types are guaranteed to be unrelated to the
            // Asset Classification smart contract. This check can happen prior to any other parsing of data inside
            // the TxEvent, which will be a minor speed increase to downstream processing
            .filter { it.eventType == WASM_EVENT_TYPE }
            .map { event -> AssetClassificationEvent(event, inputValuesEncoded = true) }
}
