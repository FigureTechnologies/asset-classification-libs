package tech.figure.classification.asset.verifier.config

import io.provenance.eventstream.net.defaultOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import okhttp3.OkHttpClient
import tech.figure.classification.asset.client.client.base.ACClient
import tech.figure.classification.asset.util.extensions.elvisAc
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import tech.figure.classification.asset.verifier.client.VerificationMessage
import tech.figure.classification.asset.verifier.client.VerifierClient
import tech.figure.classification.asset.verifier.event.AssetClassificationEventDelegator
import tech.figure.classification.asset.verifier.util.eventstream.DefaultEventStreamProvider
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Configurations to tweak the behavior of the VerifierClient created with this class.
 *
 * @param acClient Allows communication with the asset classification smart contract.  Used for retrieved scope data
 * as well as sending verification messages.
 * @param verifierAccount The account that will submit verifications when assets are successfully retrieved.  This
 * account is also used with the default onboard and verify asset event helpers to ensure that only events pertaining
 * to this account are processed.
 * @param coroutineScope The scope used to dispatch asynchronous tasks, like event processing.
 * @param verificationProcessor A manually-defined class that will process incoming assets.
 * @param eventStreamNode The URI that tells the VerifierClient where to pick up events from the Provenance Blockchain.
 * @param streamRestartMode Defines what actions to take when the event stream fails.
 * @param verificationChannel The channel that VerificationMessage values are processed through.
 * @param eventChannel The channel that VerifierEvents are processed through.  This channel handles all messages emitted
 * via the client that are routed through the AssetClassificationEventDelegator.
 * @param eventDelegator Defines handlers for different event types emitted from the asset classification smart contract.
 * @param eventProcessors All manually-defined event handling code submitted during the builder process.  See the
 * VerifierEvent class for lengthy details about each.
 * @param okHttpClientBuilder Allows for providing a custom OkHttpClient when standing up the event stream subscription.
 * This can assist in handling connection issues.
 */
class VerifierClientConfig private constructor(
    val acClient: ACClient,
    val verifierAccount: ProvenanceAccountDetail,
    val coroutineScope: CoroutineScope,
    val verificationProcessor: VerificationProcessor<*>,
    val eventStreamNode: URI,
    val streamRestartMode: StreamRestartMode,
    val verificationChannel: Channel<VerificationMessage>,
    val eventChannel: Channel<VerifierEvent>,
    val eventDelegator: AssetClassificationEventDelegator,
    val eventProcessors: Map<String, suspend (VerifierEvent) -> Unit>,
    val okHttpClientBuilder: () -> OkHttpClient,
    val eventStreamProvider: EventStreamProvider,
) {

    companion object {
        /**
         * Creation function for a VerifierClientConfig.Builder.  This should be used to build a configuration.
         * All required parameters are present in the function signature.
         *
         * @param acClient Allows communication with the asset classification smart contract.  Used for retrieved scope data
         * as well as sending verification messages.
         * @param verifierAccount The account that will submit verifications when assets are successfully retrieved.  This
         * account is also used with the default onboard and verify asset event helpers to ensure that only events pertaining
         * to this account are processed.
         * @param verificationProcessor A manually-defined class that will process incoming assets.
         */
        fun builder(
            acClient: ACClient,
            verifierAccount: ProvenanceAccountDetail,
            verificationProcessor: VerificationProcessor<*>,
        ): Builder = Builder(acClient, verifierAccount, verificationProcessor)
    }

    /**
     * Simple helper function to functionally convert a configuration into a client.
     */
    fun toClient(): VerifierClient = VerifierClient(this)

    class Builder internal constructor(
        private val acClient: ACClient,
        private val verifierAccount: ProvenanceAccountDetail,
        private val verificationProcessor: VerificationProcessor<*>,
    ) {
        private var eventStreamNode: URI? = null
        private var streamRestartMode: StreamRestartMode? = null
        private var verificationChannel: Channel<VerificationMessage>? = null
        private var eventChannel: Channel<VerifierEvent>? = null
        private var coroutineScopeConfig: VerifierCoroutineScopeConfig? = null
        private var eventDelegator: AssetClassificationEventDelegator? = null
        private val eventProcessors: MutableMap<String, suspend (VerifierEvent) -> Unit> = mutableMapOf()
        private var okHttpClientBuilder: (() -> OkHttpClient)? = null
        private var eventStreamProvider: EventStreamProvider? = null

        fun withEventStreamProvider(provider: EventStreamProvider) = apply { eventStreamProvider = provider }

        /**
         * Sets the event stream node value to listen to.  If unset, the configuration assumes the node to listen to will
         * be hosted locally.
         */
        fun withEventStreamNode(node: URI) = apply { eventStreamNode = node }

        /**
         * Defines how the verifier client will behave when the event stream fails.
         * By default, a delay of 5 seconds will occur and then the stream will restart.
         */
        fun withStreamRestartMode(mode: StreamRestartMode) = apply { streamRestartMode = mode }

        /**
         * Allows the channel on which verifications are processed to be provided manually.  This is generally
         * unnecessary, but it is exposed for more complex setups and/or testing.
         */
        fun withVerificationChannel(channel: Channel<VerificationMessage>) = apply { verificationChannel = channel }

        /**
         * Allows the channel on which events are processed to be provided manually.  This is generally unnecessary,
         * but it is exposed for more complex setups and/or testing.
         */
        fun withEventChannel(channel: Channel<VerifierEvent>) = apply { eventChannel = channel }

        /**
         * Allows a manual coroutine scope to be supplied.  By default, a scope with ten threads is supplied.
         */
        fun withCoroutineScope(config: VerifierCoroutineScopeConfig) = apply { coroutineScopeConfig = config }

        /**
         * Allows custom event handlers to be registered by using the builder process from an AssetClassificationEventDelegator.
         * The standard behavior is to process only onboard asset and verify asset events.
         */
        fun withEventDelegator(delegator: AssetClassificationEventDelegator) = apply { eventDelegator = delegator }

        // All instances of E must be able to cast to VerifierEvent due to type constraints on VerifierEventType, so this
        // cast will always succeed, despite compiler anger.
        /**
         * Allows custom events to be intercepted by the creator of the VerifierClient.  This allows actions to be taken
         * when various things occur in the VerifierClient.  See the VerifierEvent class for more details on each event.
         */
        @Suppress("UNCHECKED_CAST")
        fun <E : VerifierEvent> addEventProcessor(eventType: VerifierEventType<E>, processor: suspend (E) -> Unit) = apply {
            val eventTypeName = eventType.getEventTypeName()
            check(!eventProcessors.containsKey(eventTypeName)) { "An event of type [$eventTypeName] has already been added" }
            eventProcessors += eventTypeName to processor as suspend (VerifierEvent) -> Unit
        }

        /**
         * Allows a custom OkHttpClient to be built when starting the event stream.  If omitted, this will use the
         * default event stream OkHttpClient configurations.
         */
        fun withOkHttpClient(builder: () -> OkHttpClient) = apply { okHttpClientBuilder = builder }

        /**
         * Constructs an instance of VerifierClientConfig with all supplied values, using defaults for those not
         * supplied in the builder process.
         */
        fun build(): VerifierClientConfig = VerifierClientConfig(
            acClient = acClient,
            verifierAccount = verifierAccount,
            coroutineScope = coroutineScopeConfig.elvisAc { VerifierCoroutineScopeConfig.ScopeDefinition() }.toCoroutineScope(),
            verificationProcessor = verificationProcessor,
            eventStreamNode = eventStreamNode ?: URI("ws://localhost:26657"),
            streamRestartMode = streamRestartMode ?: StreamRestartMode.On(),
            verificationChannel = verificationChannel ?: Channel(capacity = Channel.BUFFERED),
            eventChannel = eventChannel ?: Channel(capacity = Channel.BUFFERED),
            eventDelegator = eventDelegator ?: AssetClassificationEventDelegator.default(),
            eventProcessors = eventProcessors,
            okHttpClientBuilder = okHttpClientBuilder ?: { defaultOkHttpClient() },
            eventStreamProvider = eventStreamProvider ?: DefaultEventStreamProvider(eventStreamNode ?: URI("ws://localhost:26657"), okHttpClientBuilder?.invoke() ?: defaultOkHttpClient())
        )
    }
}

/**
 * Defines the inner coroutine scope for use in the verifier, with various options for provisioning.
 */
sealed interface VerifierCoroutineScopeConfig {
    fun toCoroutineScope(): CoroutineScope

    /**
     * Use this variant if an external scope is to be provided.  This is for more complex implementations that want
     * greater control over the coroutine scope used in the verifier.
     */
    class ProvidedScope(private val scope: CoroutineScope) : VerifierCoroutineScopeConfig {
        override fun toCoroutineScope(): CoroutineScope = scope
    }

    /**
     * This is the default variant.  It automatically creates a coroutine scope with the given number of dedicated
     * threads.
     */
    class ScopeDefinition(
        private val scopeName: String = "verifier-scope",
        private val threadCount: Int = 10,
    ) : VerifierCoroutineScopeConfig {
        override fun toCoroutineScope(): CoroutineScope = Executors.newFixedThreadPool(threadCount, NamedThreadFactory(scopeName))
            .asCoroutineDispatcher()
            .plus(SupervisorJob())
            .let(::CoroutineScope)
    }
}

/**
 * A simple ThreadFactory implementation for creating a batch of threads.
 */
private class NamedThreadFactory(private val prefix: String) : ThreadFactory {
    private val sequence = AtomicInteger(1)

    override fun newThread(r: Runnable?): Thread {
        val thread = Thread(r)
        val seq = sequence.getAndIncrement()
        thread.name = "$prefix${if (seq > 1) "-$seq" else ""}"
        thread.isDaemon = true
        return thread
    }
}
