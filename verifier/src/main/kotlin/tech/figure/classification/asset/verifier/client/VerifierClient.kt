package tech.figure.classification.asset.verifier.client

import cosmos.auth.v1beta1.Auth.BaseAccount
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.getBaseAccount
import io.provenance.client.protobuf.extensions.getTx
import io.provenance.eventstream.decoder.moshiDecoderAdapter
import io.provenance.eventstream.net.okHttpNetAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.figure.classification.asset.client.client.base.BroadcastOptions
import tech.figure.classification.asset.client.domain.execute.VerifyAssetExecute
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.util.extensions.alsoIfAc
import tech.figure.classification.asset.util.extensions.toProvenanceTxEventsAc
import tech.figure.classification.asset.util.wallet.AccountSigner
import tech.figure.classification.asset.verifier.config.StreamRestartMode
import tech.figure.classification.asset.verifier.config.VerificationProcessor
import tech.figure.classification.asset.verifier.config.VerifierClientConfig
import tech.figure.classification.asset.verifier.config.VerifierEvent
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredUnhandledEventType
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredUnknownWasmEvent
import tech.figure.classification.asset.verifier.config.VerifierEvent.NewBlockHeightReceived
import tech.figure.classification.asset.verifier.config.VerifierEvent.NewBlockReceived
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamCompleted
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamExceptionOccurred
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamExited
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamRestarted
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendFailed
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendSucceeded
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendSyncSequenceNumberFailed
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendThrewException
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventChannelThrewException
import tech.figure.classification.asset.verifier.config.VerifierEventType
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import tech.figure.classification.asset.verifier.util.eventstream.verifierBlockDataFlow
import java.util.concurrent.atomic.AtomicLong

class VerifierClient(private val config: VerifierClientConfig) {
    // Cast the provided processor to T of Any to make creation and usage easier on the consumer of this library
    @Suppress("UNCHECKED_CAST")
    private val verifyProcessor: VerificationProcessor<Any> = config.verificationProcessor as VerificationProcessor<Any>
    private val signer = AccountSigner.fromAccountDetail(config.verifierAccount)
    private val decoderAdapter = moshiDecoderAdapter()
    private var jobs = VerifierJobs()
    private val tracking: AccountTrackingDetail by lazy {
        AccountTrackingDetail.lookup(
            pbClient = config.acClient.pbClient,
            address = config.verifierAccount.bech32Address,
        )
    }

    fun manualVerifyHash(txHash: String) {
        val tx = config.acClient.pbClient.cosmosService.getTx(txHash)
        val events = AssetClassificationEvent.fromVerifierTxEvents(
            sourceTx = tx,
            txEvents = tx.txResponse.toProvenanceTxEventsAc(),
        )
        config.coroutineScope.launch {
            events.forEach { acEvent -> handleEvent(acEvent) }
        }
    }

    fun startVerifying(startingBlockHeight: Long): Job = config
        .coroutineScope
        .launch { verifyLoop(startingBlockHeight) }
        .also { jobs.processorJob = it }
        .also { startEventChannelReceiver() }
        .also { startVerificationReceiver() }

    fun stopVerifying() {
        jobs.cancelAndClearJobs()
        tracking.reset()
    }

    fun restartVerifier(startingBlockHeight: Long): Job {
        stopVerifying()
        return startVerifying(startingBlockHeight)
    }

    private suspend fun verifyLoop(startingBlockHeight: Long?) {
        val netAdapter = okHttpNetAdapter(
            node = config.eventStreamNode.toString(),
            okHttpClient = config.okHttpClientBuilder(),
        )
        val currentHeight = netAdapter.rpcAdapter.getCurrentHeight()
        var latestBlock = startingBlockHeight?.takeIf { start -> start > 0 && currentHeight?.let { it >= start } != false }
        verifierBlockDataFlow(netAdapter, decoderAdapter, from = latestBlock)
            .catch { e -> StreamExceptionOccurred(e).send() }
            .onCompletion { t -> StreamCompleted(t).send() }
            .onEach { block ->
                // Record each block intercepted
                NewBlockReceived(block).send()
                // Track new block height
                latestBlock = trackBlockHeight(latestBlock, block.height)
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
            StreamExceptionOccurred(e).send()
            // Escape the loop entirely if the adapter fails to shut down - there should never be two adapters running
            // in tandem via this client
            return
        }
        when (config.streamRestartMode) {
            is StreamRestartMode.On -> {
                if (config.streamRestartMode.restartDelayMs > 0) {
                    delay(config.streamRestartMode.restartDelayMs)
                }
                StreamRestarted(latestBlock).send()
                // Recurse into a new event stream if the stream needs to restart
                verifyLoop(latestBlock)
            }
            is StreamRestartMode.Off -> {
                StreamExited(latestBlock).send()
            }
        }
    }

    private suspend fun trackBlockHeight(
        latestHeight: Long?,
        newHeight: Long
    ): Long = if (latestHeight == null || latestHeight < newHeight) {
        NewBlockHeightReceived(newHeight).send()
        newHeight
    } else {
        latestHeight
    }

    internal suspend fun handleEvent(event: AssetClassificationEvent) {
        // This will be extremely common - we cannot filter events upfront in the event stream code, so this check
        // throws away everything not emitted by the asset classification smart contract
        if (event.eventType == null) {
            EventIgnoredUnknownWasmEvent(event).send()
            return
        }
        // Only handle events that are relevant to the verifier
        if (event.eventType !in config.eventDelegator.getHandledEventTypes()) {
            EventIgnoredUnhandledEventType(event).send()
            return
        }
        config.eventDelegator.delegateEvent(
            parameters = EventHandlerParameters(
                event = event,
                acClient = config.acClient,
                verifierAccount = config.verifierAccount,
                processor = verifyProcessor,
                verificationChannel = config.verificationChannel,
                eventChannel = config.eventChannel,
            )
        )
    }

    internal fun startVerificationReceiver() {
        // Only one receiver channel needs to run at a time
        if (jobs.verificationSendJob != null) {
            return
        }
        config.coroutineScope.launch {
            // A for-loop over a channel will infinitely iterate
            for (message in config.verificationChannel) {
                try {
                    val response = try {
                        config.acClient.verifyAsset(
                            execute = VerifyAssetExecute(
                                identifier = AssetIdentifier.ScopeAddress(message.scopeAttribute.scopeAddress),
                                success = message.verification.success,
                                message = message.verification.message,
                                accessRoutes = message.verification.accessRoutes,
                            ),
                            signer = signer,
                            options = BroadcastOptions(
                                broadcastMode = BroadcastMode.BROADCAST_MODE_SYNC,
                                baseAccount = tracking.sequencedAccount(incrementAfterGet = true),
                            )
                        )
                    } catch (t: Throwable) {
                        VerifyAssetSendThrewException(
                            event = message.event,
                            scopeAttribute = message.scopeAttribute,
                            verification = message.verification,
                            message = "${message.failureMessagePrefix} Sending verification to smart contract failed",
                            t = t,
                        ).send()
                        try {
                            tracking.reset()
                        } catch (t: Throwable) {
                            VerifyAssetSendSyncSequenceNumberFailed(
                                event = message.event,
                                scopeAttribute = message.scopeAttribute,
                                verification = message.verification,
                                message = "${message.failureMessagePrefix} Failed to reset account data after transaction. This may require an app restart",
                                t = t,
                            ).send()
                        }
                        null
                    }
                    response?.also {
                        if (response.txResponse.code == 0) {
                            VerifyAssetSendSucceeded(
                                event = message.event,
                                scopeAttribute = message.scopeAttribute,
                                verification = message.verification,
                            ).send()
                        } else {
                            VerifyAssetSendFailed(
                                event = message.event,
                                scopeAttribute = message.scopeAttribute,
                                verification = message.verification,
                                responseCode = response.txResponse.code,
                                rawLog = response.txResponse.rawLog,
                            ).send()
                        }
                    }
                } catch (t: Throwable) {
                    VerifyEventChannelThrewException(t).send()
                }
            }
        }.also { jobs.verificationSendJob = it }
    }

    internal fun startEventChannelReceiver() {
        // Only one receiver channel needs to run at a time
        if (jobs.eventHandlerJob != null) {
            return
        }
        config.coroutineScope.launch {
            // A for-loop over a channel will infinitely iterate
            for (event in config.eventChannel) {
                try {
                    config.eventProcessors[event.getEventTypeName()]?.invoke(event)
                } catch (t: Throwable) {
                    try {
                        config.eventProcessors[VerifierEventType.EventProcessorFailed.getEventTypeName()]
                            ?.invoke(VerifierEvent.EventProcessorFailed(failedEventName = event.getEventTypeName(), t = t))
                    } catch (t: Throwable) {
                        // Worst case scenario - bad event with bad custom event handler.  This just gets silently
                        // ignored because there's nothing that can be done.
                    }
                }
            }
        }.also { jobs.eventHandlerJob = it }
    }

    private suspend fun VerifierEvent.send() {
        config.eventChannel.send(this)
    }
}

private data class VerifierJobs(
    var processorJob: Job? = null,
    var verificationSendJob: Job? = null,
    var eventHandlerJob: Job? = null,
) {
    fun cancelAndClearJobs() {
        processorJob?.cancel(CancellationException("Manual verification cancellation requested"))
        verificationSendJob?.cancel(CancellationException("Manual verification sender job cancellation requested"))
        eventHandlerJob?.cancel(CancellationException("Manual event handler job cancellation requested"))
        processorJob = null
        verificationSendJob = null
        eventHandlerJob = null
    }
}

private data class AccountTrackingDetail(
    val pbClient: PbClient,
    private var account: BaseAccount,
    private val sequenceNumber: AtomicLong,
) {
    companion object {
        fun lookup(pbClient: PbClient, address: String): AccountTrackingDetail = pbClient.authClient.getBaseAccount(address).let { account ->
            AccountTrackingDetail(
                pbClient = pbClient,
                account = account,
                sequenceNumber = account.sequence.let(::AtomicLong),
            )
        }
    }

    fun sequencedAccount(incrementAfterGet: Boolean = false): BaseAccount = account
        .toBuilder()
        .setSequence(sequenceNumber.get())
        .build()
        .alsoIfAc(incrementAfterGet) { addTransaction() }

    fun reset() {
        account = pbClient.authClient.getBaseAccount(account.address).also { sequenceNumber.set(it.sequence) }
    }

    fun addTransaction() {
        sequenceNumber.incrementAndGet()
    }
}
