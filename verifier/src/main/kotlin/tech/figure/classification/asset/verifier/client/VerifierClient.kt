package tech.figure.classification.asset.verifier.client

import cosmos.auth.v1beta1.Auth.BaseAccount
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.getBaseAccount
import io.provenance.client.protobuf.extensions.getTx
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.figure.classification.asset.client.client.base.BroadcastOptions
import tech.figure.classification.asset.client.domain.execute.VerifyAssetExecute
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.util.extensions.alsoIfAc
import tech.figure.classification.asset.util.extensions.toProvenanceTxEventsAc
import tech.figure.classification.asset.util.wallet.AccountSigner
import tech.figure.classification.asset.verifier.config.RecoveryStatus
import tech.figure.classification.asset.verifier.config.StreamRestartMode
import tech.figure.classification.asset.verifier.config.VerificationProcessor
import tech.figure.classification.asset.verifier.config.VerifierClientConfig
import tech.figure.classification.asset.verifier.config.VerifierEvent
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredContractMismatch
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredUnhandledEventType
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredUnknownWasmEvent
import tech.figure.classification.asset.verifier.config.VerifierEvent.NewBlockHeightReceived
import tech.figure.classification.asset.verifier.config.VerifierEvent.NewBlockReceived
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamCompleted
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamExceptionOccurred
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamExited
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamRestarted
import tech.figure.classification.asset.verifier.config.VerifierEvent.StreamRestarting
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendFailed
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendSucceeded
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendSyncSequenceNumberFailed
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyAssetSendThrewException
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventChannelThrewException
import tech.figure.classification.asset.verifier.config.VerifierEventType
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import java.util.concurrent.atomic.AtomicLong

class VerifierClient(private val config: VerifierClientConfig) {
    // Cast the provided processor to T of Any to make creation and usage easier on the consumer of this library
    @Suppress("UNCHECKED_CAST")
    private val verifyProcessor: VerificationProcessor<Any> = config.verificationProcessor as VerificationProcessor<Any>
    private val signer = AccountSigner.fromAccountDetail(config.verifierAccount)
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
        .takeUnless { jobs.jobsAreActive() }
        ?.launch { verifyLoop(startingBlockHeight) }
        ?.also { jobs.processorJob = it }
        ?.also { startEventChannelReceiver() }
        ?.also { startVerificationReceiver() }
        ?: jobs.processorJob
        ?: throw IllegalStateException("Expected processor job to be running but it was not. Please try again.")

    fun stopVerifying() {
        jobs.cancelAndClearJobs()
        tracking.reset()
    }

    fun restartVerifier(startingBlockHeight: Long): Job {
        stopVerifying()
        return startVerifying(startingBlockHeight)
    }

    private suspend fun verifyLoop(
        startingBlockHeight: Long?,
        retry: BlockRetry = BlockRetry(block = startingBlockHeight),
    ) {
        val currentHeight = config.eventStreamProvider.currentHeight()
        var latestBlock = startingBlockHeight?.takeIf { start -> start > 0 && currentHeight?.let { it >= start } != false }

        val recoverable = config.eventStreamProvider.startProcessingFromHeight(
            latestBlock,
            onBlock = { blockHeight ->
                // Record each block intercepted
                NewBlockReceived(blockHeight).send()
                // Track new block height
                latestBlock = trackBlockHeight(latestBlock, blockHeight)
            },
            onEvent = { event -> handleEvent(event) },
            onError = { e -> StreamExceptionOccurred(e).send() },
            onCompletion = { t -> StreamCompleted(t).send() },
        )

        when (config.streamRestartMode) {
            is StreamRestartMode.On -> {
                if (recoverable == RecoveryStatus.RECOVERABLE) {
                    // Use the retry count recorded in the retry parameter if the client is stuck on a specific block.  If
                    // the client is not stuck on the same block, then reset the counter to zero to start a new set of
                    // retries, ensuring that various retries throughout iteration through blocks do not infinitely increase
                    // the delay unless reading the chain has become forever halted
                    val retryCount = retry.retryCount.takeIf { retry.block == latestBlock } ?: 0
                    val restartDelayDuration = config.streamRestartMode.calcDelay(retryCount)
                    // Note that the stream is restarting before the delay occurs to ensure consumers know the state of the
                    // flow is about to begin again from the latest block recorded
                    StreamRestarting(
                        restartHeight = latestBlock,
                        restartCount = retryCount + 1,
                        restartDelayMs = restartDelayDuration.inWholeMilliseconds,
                    ).send()
                    delay(restartDelayDuration)
                    // Note that the stream delay is over and the loop is about to restart
                    StreamRestarted(latestBlock, retryCount + 1).send()
                    // Recurse into a new event stream if the stream needs to restart
                    verifyLoop(
                        startingBlockHeight = latestBlock,
                        retry = BlockRetry(
                            retryCount = retryCount + 1,
                            block = latestBlock,
                        ),
                    )
                } else {
                    StreamExited(latestBlock).send()
                }
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
        // If multiple asset classification smart contracts are instantiated and used in the same Provenance Blockchain
        // environment, their events will all be detected by this client.  This check avoids attempting to process
        // correctly-formatted contract events from a wholly different contract than the one registered in the configuration's
        // ACClient instance
        config.acClient.queryContractAddress().also { contractAddress ->
            if (event.contractAddress != contractAddress) {
                EventIgnoredContractMismatch(
                    event = event,
                    message = "This client instance watches contract address [$contractAddress], but this event originated from address [${event.contractAddress}]",
                ).send()
                return
            }
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
                                assetType = message.scopeAttribute.assetType,
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

    fun jobsAreActive(): Boolean = processorJob != null || verificationSendJob != null || eventHandlerJob != null
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

private data class BlockRetry(
    val retryCount: Long = 0,
    val block: Long? = null,
)
