package tech.figure.classification.asset.verifier.event

import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredUnknownEvent
import tech.figure.classification.asset.verifier.event.defaults.DefaultOnboardEventHandler
import tech.figure.classification.asset.verifier.event.defaults.DefaultVerifyAssetEventHandler
import tech.figure.classification.asset.verifier.provenance.ACContractEvent

/**
 * Contains a collection of event handlers that allow processing of an AssetClassificationEvent based on its eventType.
 * Allows one handler per ACContractEvent variant.
 */
class AssetClassificationEventDelegator internal constructor(
    private val eventHandlers: Map<ACContractEvent, AssetClassificationEventHandler>
) {
    companion object {
        /**
         * Creates a basic instance of this builder without any default values.
         */
        fun builder(): AssetClassificationEventDelegatorBuilder = AssetClassificationEventDelegatorBuilder()

        /**
         * Creates an instance of this builder with the provided implementations for ONBOARD_ASSET and VERIFY_ASSET.
         */
        fun defaultBuilder(): AssetClassificationEventDelegatorBuilder = builder()
            .registerEventHandler(DefaultOnboardEventHandler)
            .registerEventHandler(DefaultVerifyAssetEventHandler)

        /**
         * Builds a default instance of AssetClassificationEventDelegator with a provided implementation for
         * ONBOARD_ASSET and VERIFY_ASSET.
         */
        fun default(): AssetClassificationEventDelegator = defaultBuilder().build()
    }

    /**
     * A simple builder class that allows fluent addition of implementations of AssetClassificationEventHandler.
     */
    class AssetClassificationEventDelegatorBuilder internal constructor() {
        private val eventHandlers: MutableMap<ACContractEvent, AssetClassificationEventHandler> = mutableMapOf()

        /**
         * Adds the given event handler to the internal map.  Will reject subsequent additions of values with
         * existing types.  As such, take care in which creation function is used for the instance of
         * AssetClassificationEventDelegatorBuilder.  For instance, using AssetClassificationEventDelegator.defaultBuilder()
         * and then attempting to add a new implementation of the ONBOARD_ASSET event will throw an exception.
         */
        fun <T : AssetClassificationEventHandler> registerEventHandler(handler: T) = apply {
            check(handler.eventType !in eventHandlers.keys) {
                "Attempted to register more than a single handler for type [${handler.eventType}]. Previously-registered instance name: ${eventHandlers[handler.eventType]!!::class.simpleName}"
            }
            eventHandlers += handler.eventType to handler
        }

        /**
         * Constructs an instance of an AssetClassificationEventDelegator using the inner map provided.
         */
        fun build(): AssetClassificationEventDelegator = AssetClassificationEventDelegator(eventHandlers)
    }

    /**
     * Simple exposure function to get all the different classification contract event types registered.
     */
    fun getHandledEventTypes(): Set<ACContractEvent> = eventHandlers.keys

    /**
     * The main functionality provided by the class.  Takes the EventHandlerParameters, built by the VerifierClient,
     * and determines if an event handler is registered.  If it is, the handleEvent function is run for the handler.
     * If not, an EventIgnoredUnknownEvent message is sent to the eventChannel for handling.
     */
    suspend fun delegateEvent(parameters: EventHandlerParameters) {
        eventHandlers[parameters.event.eventType]
            ?.handleEvent(parameters)
            ?: run { parameters.eventChannel.send(EventIgnoredUnknownEvent(parameters.event)) }
    }
}
