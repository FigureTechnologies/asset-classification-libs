package tech.figure.classification.asset.verifier.config

import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.client.domain.model.AssetScopeAttribute
import tech.figure.classification.asset.verifier.client.AssetVerification
import tech.figure.classification.asset.verifier.provenance.ACContractEvent
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent

/**
 * A collection of events emitted by the VerifierClient.  These events can all be dynamically intercepted and handled
 * when creating a [VerifierClientConfig] for a [VerifierClient][tech.figure.classification.asset.verifier.client.VerifierClient].
 */
sealed interface VerifierEvent {
    /**
     * A shortcut to get the simple name of the class, which should always match its [VerifierEventType] counterpart,
     * ensuring that the [VerifierClientConfig.Builder.addEventProcessor] function does not create a bad link.
     */
    fun getEventTypeName(): String = this::class.simpleName!!

    /**
     * When the event stream encounters an exception (uncommon unless the blockchain is having issues), this function
     * will be called with the encountered exception.
     *
     * @param t The exception encountered during stream processing.
     */
    data class StreamExceptionOccurred internal constructor(val t: Throwable) : VerifierEvent

    /**
     * When the event stream completes processing blocks (when it reaches the current block height), the flow will be
     * marked as completed and this function will execute.  If the stream completed exceptionally, the throwable herein
     * will be non-null, but it's very common for the stream to complete normally and the throwable to be null.
     *
     * @param t An exception encountered when the stream completed.  It is completely possible for the stream to
     * complete without throwing an exception.
     */
    data class StreamCompleted internal constructor(val t: Throwable?) : VerifierEvent

    /**
     * This is a catch-all function that emits each and every block height that the verifier encounters.  The verifier will
     * filter down all events to only check "wasm" event types, so only those will be encountered herein.  However,
     * this function occurs before any sanity checks in the verifier codebase, so these block heights may or may not pertain
     * to the registered verifier account in the VerifierClientConfig.
     *
     * @param blockHeight The [Long] most recent block height encountered by the stream processor.
     */
    data class NewBlockReceived internal constructor(val blockHeight: Long) : VerifierEvent

    /**
     * After the stream completes, if the stream has been configured to restart, this function will be called.  This
     * denotes the delay that will be used and the number of restarts that have previously occurred.  This event will
     * occur before the restart delay occurs.
     *
     * @param restartHeight The block height at which the stream had reached when the restart was requested.  This will
     * only be null if the stream restarts before a block height can ever be stored, which will only happen if the
     * processor for blocks had never connected and found any data.
     * @param restartCount The number of times that a restart has occurred.
     * @param restartDelayMs The calculated backoff milliseconds that the verifier will wait before restarting the block
     * flow.
     */
    data class StreamRestarting internal constructor(val restartHeight: Long?, val restartCount: Long, val restartDelayMs: Long) : VerifierEvent

    /**
     * After the stream completes, if the stream has been configured to restart, this function will be called.  This
     * function can be assumed to run immediately before a new instance of the stream starts with a new block height.
     * This event will occur after the restart delay occurs.
     *
     * @param restartHeight The block height to which the stream had reached when the restart occurred.  This will only
     * be null if the stream restarts before a block height can ever be stored, which will only happen if the processor
     * for blocks had never connected and found any data.
     * @param restartCount The number of times that a restart has occurred.
     */
    data class StreamRestarted internal constructor(val restartHeight: Long?, val restartCount: Long) : VerifierEvent

    /**
     * If the verifier is configured to not restart, this function will be called after the stream completes.  It
     * signifies that no more events will be processed until the verifier is manually started again.
     *
     * @param exitHeight The block height to which the stream had reached when the exit occurred.  This will only be
     * null if the stream exits before any blocks are processed.
     */
    data class StreamExited internal constructor(val exitHeight: Long?) : VerifierEvent

    /**
     * When the client registers a new block height after iterating over encountered messages in its Flow, this function
     * will be called.  The events are processed in an ordered manner, so this function will only be called with larger
     * values than a value in a previous call.  This function is useful for registering previously-encountered block
     * heights with an external registrar for maintaining app state for app restarts.
     *
     * @param newHeight The new block height encountered by the client.
     */
    data class NewBlockHeightReceived internal constructor(val newHeight: Long) : VerifierEvent

    /**
     * This will be extremely common - we cannot filter events upfront in the event stream code, so this check
     * throws away everything not emitted by the asset classification smart contract.  This can be safely ignored, but
     * is available for debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredUnknownWasmEvent internal constructor(
        val event: AssetClassificationEvent,
        val message: String = "Unknown wasm event encountered"
    ) : VerifierEvent

    /**
     * This event indicates that a parsed classification event on the event stream was originated from a different
     * asset classification smart contract than the one configured to be watched by the VerifierClient.  This will be
     * very common in an environment that includes multiple contract instances and can be safely ignored.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredContractMismatch internal constructor(
        val event: AssetClassificationEvent,
        val message: String
    ) : VerifierEvent

    /**
     * Only handle events that are relevant to the verifier.  The asset classification smart contract emits many more
     * events than the verifier client needs to handle, and this event is triggered when one of those events occur.
     * This can be safely ignored, but is available for debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredUnhandledEventType internal constructor(
        val event: AssetClassificationEvent,
        val message: String = "Event type is not handled by the verifier"
    ) : VerifierEvent

    /**
     * This should never happen.  It indicates a change was made to the asset classification smart contract and/or the
     * underlying event processing that allowed an event that was not a handled type (ACContractEvent.HANDLED_EVENTS)
     * to pass into the event type when switch.  This event should be considered an error case and any external error
     * handling should be used to alert of this error.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredUnknownEvent internal constructor(
        val event: AssetClassificationEvent,
        val message: String = "After all event checks, an unexpected event was attempted for processing. Tx hash: [${event.sourceEvent.txHash}], event type: [${event.eventType}]"
    ) : VerifierEvent

    /**
     * This event is emitted when the verifier client picks up an event that has already completed being verified.  This
     * should only happen when events are re-run by crawling old data in the event stream.  This can safely be ignored
     * unless interactions need to occur when encountering a completed event.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param message A message indicating the nature of the event.
     */
    data class OnboardEventIgnoredPreviouslyProcessed internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val message: String
    ) : VerifierEvent

    /**
     * This event is emitted when the VerificationProcessor's defined method of fetching an asset from a scope attribute's
     * data fails and throws an exception.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param t The exception thrown when attempting to use the [VerificationProcessor] to fetch an asset.
     */
    data class OnboardEventFailedToRetrieveAsset internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val t: Throwable
    ) : VerifierEvent

    /**
     * This event is emitted when the VerificationProcessor's defined method of verifying asset data from a fetched
     * asset fails and throws an exception.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param t The exception thrown when attempting to use the [VerificationProcessor] to get verification details.
     */
    data class OnboardEventFailedToVerifyAsset internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val t: Throwable
    ) : VerifierEvent

    /**
     * This event always triggers before verification a verification transaction is sent to the smart contract for a
     * scope.  This can safely be ignored unless a pre verification hook is needed.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     */
    data class OnboardEventPreVerifySend internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val verification: AssetVerification
    ) : VerifierEvent

    /**
     * This event is emitted when an asset classification smart contract "verify asset" event is detected and its
     * new onboarding status is not in an expected state.  This is an error case that indicates that although a
     * verification event was run, the resulting scope attribute is now in an unexpected state.  This can theoretically
     * happen if verification has been denied and then onboarding has been once again invoked, but the likelihood of
     * that occurrence versus bad data is low.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    data class VerifyEventUnexpectedOnboardingStatus internal constructor(
        val event: AssetClassificationEvent,
        val message: String
    ) : VerifierEvent

    /**
     * This event is emitted when an asset classification smart contract "verify asset" event is processed and has
     * successfully moved the scope attribute's status to either approved or denied.  This event should be considered
     * the ideal ending process for all scopes that pass through the asset classification smart contract.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param newOnboardingStatus The new status that the asset has been moved to after successful onboarding.
     */
    data class VerifyEventSuccessful internal constructor(
        val event: AssetClassificationEvent,
        val newOnboardingStatus: AssetOnboardingStatus
    ) : VerifierEvent

    /**
     * This event is emitted when an exception occurs attempting to send an asset verification to the smart contract
     * with the ACClient instance.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     * @param message A message indicating the nature of the event.
     * @param t The exception thrown.
     */
    data class VerifyAssetSendThrewException internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val verification: AssetVerification,
        val message: String,
        val t: Throwable
    ) : VerifierEvent

    /**
     * This event is emitted when the verifier attempts to re-synchronize its sequence number with the registered
     * verifier account's values on the blockchain, and that process throws an exception.  This indicates that blockchain
     * communication is currently failing and that the verifier will be unable to successfully submit transactions.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     * @param message A message indicating the nature of the event.
     * @param t The exception thrown when attempting to query account information for the registered verifier address.
     */
    data class VerifyAssetSendSyncSequenceNumberFailed internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val verification: AssetVerification,
        val message: String,
        val t: Throwable
    ) : VerifierEvent

    /**
     * This event is emitted when the asset classification smart contract receives and processes a verification.  The
     * "verify asset" event processing must still collect the event and verify the status of the asset scope attribute
     * before considering the verification process to be complete for the target asset, but this event at least
     * signifies that the transaction to verify the asset was processed.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     */
    data class VerifyAssetSendSucceeded internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val verification: AssetVerification
    ) : VerifierEvent

    /**
     * This event is emitted when attempting to verify the asset in a transaction fails.  The response includes the
     * information returned via the Provenance Blockchain about the problem.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     * @param responseCode The response code received from the Provenance Blockchain.  A zero indicates a success, so
     * this value is guaranteed to be non-zero when this event is encountered.
     * @param rawLog The log text received from the Provenance Blockchain, generally indicating the nature of the
     * error encountered that caused the verification message to not be processed.
     */
    data class VerifyAssetSendFailed internal constructor(
        val event: AssetClassificationEvent,
        val scopeAttribute: AssetScopeAttribute,
        val verification: AssetVerification,
        val responseCode: Int,
        val rawLog: String
    ) : VerifierEvent

    /**
     * This event is emitted when the coroutine channel used to process verifier events sequentially throws an exception.
     * Any number of coroutine/async things can occur that can cause this error when the environment is having difficulties
     * and it should be watched in a robust setup.
     *
     * @param t The exception thrown.
     */
    data class VerifyEventChannelThrewException internal constructor(val t: Throwable) : VerifierEvent

    /**
     * This will commonly happen - the contract emits events that don't target the verifier at all, but they'll
     * still pass through here.  This can be safely ignored, but is available for debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredNoVerifierAddress internal constructor(
        val event: AssetClassificationEvent,
        val eventType: ACContractEvent,
        val message: String = "Event does not contain a verifier address"
    ) : VerifierEvent

    /**
     * Only process verifications that are targeted at the registered verifier account.  Other verifiers can be chosen,
     * and those events will still pass through the verifier client.  This can be safely ignored, but is available for
     * debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param registeredVerifierAddress The address of the verifier that is currently held by the client.  This is
     * used to make the message produced more contextually relevant.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredDifferentVerifierAddress internal constructor(
        val event: AssetClassificationEvent,
        val eventType: ACContractEvent,
        val registeredVerifierAddress: String,
        val message: String = "Event is for a different verifier [${event.verifierAddress}] than the registered verifier account [$registeredVerifierAddress]"
    ) : VerifierEvent

    /**
     * This event is emitted when an asset classification smart contract event is detected, but it does not contain the
     * expected scope address attribute.  This is an error case and indicates bad smart contract code.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredMissingScopeAddress(
        val event: AssetClassificationEvent,
        val eventType: ACContractEvent,
        val message: String
    ) : VerifierEvent

    /**
     * This event is emitted when an asset classification smart contract event is detected, but it does not contain the
     * expected asset type for an onboarded scope.  This is an error case and indicates bad smart contract code.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param message A message indicating the nature of the event.
     */
    data class EventIgnoredMissingAssetType(
        val event: AssetClassificationEvent,
        val eventType: ACContractEvent,
        val message: String
    ) : VerifierEvent

    /**
     * This event is emitted when an asset classification smart contract event is detected, but the scope
     * that was onboarded does not yet have an AssetScopeAttribute on it.  This should only ever occur due to a
     * blockchain error, or indicates a smart contract bug.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The type of smart contract event that was encountered when the scope attribute was missing.
     * @param message A message indicating the nature of the event.
     * @param t The exception thrown when attempting to fetch the scope attribute from the smart contract.  Indicates
     * the reason for the missing attribute.
     */
    data class EventIgnoredMissingScopeAttribute(
        val event: AssetClassificationEvent,
        val eventType: ACContractEvent,
        val message: String,
        val t: Throwable
    ) : VerifierEvent

    /**
     * This event is emitted when an event processor is registered in the VerifierClientConfig that can throw an exception.
     * The VerifierClient will catch any exception thrown by custom processor code and emit this error with the
     * exception thrown contained as `t`.  It is HIGHLY recommended that only safe code be run in the handler for this
     * event, because if the code in the handler fails with an exception, the problem is silently ignored.
     *
     * @param failedEventName The name of the [VerifierEvent] that had a misconfigured custom event process that threw
     * an exception.
     * @param t The exception thrown during the event processor's invocation.
     */
    data class EventProcessorFailed internal constructor(
        val failedEventName: String,
        val t: Throwable
    ) : VerifierEvent

    /**
     * This event allows an external consumer of this library to create their own event handlers with as many custom
     * event types as they want.  It behaves differently in the event processing code, keying off of the [eventName]
     * property, versus keying off of its class name.
     *
     * @param eventName The unique event type identifier key.  This value should be different for each custom event.
     * It is recommended that an enum be used for each different custom event to ensure uniqueness, but this is entirely
     * up to the consumer of the library to decide.
     * @param eventBody Any T type that needs to be emitted in the event.  This will likely be a data class containing
     * many additional fields.
     */
    data class CustomEvent<T>(val eventName: String, val eventBody: T) : VerifierEvent {
        override fun getEventTypeName(): String = eventName
    }
}

/**
 * This interface allows for relatively simple syntax for adding a processor for an event type in the [VerifierClientConfig].
 * This is all wired together by collecting a map of class names in String form, and works because all [VerifierEventType]
 * values are named exactly the same as their [VerifierEvent] counterparts.  The generic parameter of [VerifierEvent]
 * allows the [VerifierClientConfig.Builder.addEventProcessor] function to infer the [VerifierEvent] that they refer to
 * and dynamically change the receiver of the input function.
 */
sealed interface VerifierEventType<E : VerifierEvent> {
    /**
     * A shortcut to get the simple name of the class, which should always match its [VerifierEvent] counterpart,
     * ensuring that the [VerifierClientConfig.Builder.addEventProcessor] function does not create a bad link.
     */
    fun getEventTypeName(): String = this::class.simpleName!!

    /**
     * When the event stream encounters an exception (uncommon unless the blockchain is having issues), this function
     * will be called with the encountered exception.
     *
     * @param t The exception encountered during stream processing.
     */
    object StreamExceptionOccurred : VerifierEventType<VerifierEvent.StreamExceptionOccurred>

    /**
     * When the event stream completes processing blocks (when it reaches the current block height), the flow will be
     * marked as completed and this function will execute.  If the stream completed exceptionally, the throwable herein
     * will be non-null, but it's very common for the stream to complete normally and the throwable to be null.
     *
     * @param t An exception encountered when the stream completed.  It is completely possible for the stream to
     * complete without throwing an exception.
     */
    object StreamCompleted : VerifierEventType<VerifierEvent.StreamCompleted>

    /**
     * This is a catch-all function that emits each and every block height that the verifier encounters.  The verifier will
     * filter down all events to only check "wasm" event types, so only those will be encountered herein.  However,
     * this function occurs before any sanity checks in the verifier codebase, so these block heights may or may not pertain
     * to the registered verifier account in the VerifierClientConfig.
     *
     * @param blockHeight The [Long] most recent block height encountered by the stream processor.
     */
    object NewBlockReceived : VerifierEventType<VerifierEvent.NewBlockReceived>

    /**
     * After the stream completes, if the stream has been configured to restart, this function will be called.  This
     * denotes the delay that will be used and the number of restarts that have previously occurred.  This event will
     * occur before the restart delay occurs.
     *
     * @param restartHeight The block height at which the stream had reached when the restart was requested.  This will
     * only be null if the stream restarts before a block height can ever be stored, which will only happen if the
     * processor for blocks had never connected and found any data.
     * @param restartCount The number of times that a restart has occurred.
     * @param restartDelayMs The calculated backoff milliseconds that the verifier will wait before restarting the block
     * flow.
     */
    object StreamRestarting : VerifierEventType<VerifierEvent.StreamRestarting>

    /**
     * After the stream completes, if the stream has been configured to restart, this function will be called.  This
     * function can be assumed to run immediately before a new instance of the stream starts with a new block height.
     * This event will occur after the restart delay occurs.
     *
     * @param restartHeight The block height to which the stream had reached when the restart occurred.  This will only
     * be null if the stream restarts before a block height can ever be stored, which will only happen if the processor
     * for blocks had never connected and found any data.
     * @param restartCount The number of times that a restart has occurred.
     */
    object StreamRestarted : VerifierEventType<VerifierEvent.StreamRestarted>

    /**
     * If the verifier is configured to not restart, this function will be called after the stream completes.  It
     * signifies that no more events will be processed until the verifier is manually started again.
     *
     * @param exitHeight The block height to which the stream had reached when the exit occurred.  This will only be
     * null if the stream exits before any blocks are processed.
     */
    object StreamExited : VerifierEventType<VerifierEvent.StreamExited>

    /**
     * When the client registers a new block height after iterating over encountered messages in its Flow, this function
     * will be called.  The events are processed in an ordered manner, so this function will only be called with larger
     * values than a value in a previous call.  This function is useful for registering previously-encountered block
     * heights with an external registrar for maintaining app state for app restarts.
     *
     * @param newHeight The new block height encountered by the client.
     */
    object NewBlockHeightReceived : VerifierEventType<VerifierEvent.NewBlockHeightReceived>

    /**
     * This will be extremely common - we cannot filter events upfront in the event stream code, so this check
     * throws away everything not emitted by the asset classification smart contract.  This can be safely ignored, but
     * is available for debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredUnknownWasmEvent : VerifierEventType<VerifierEvent.EventIgnoredUnknownWasmEvent>

    /**
     * This event indicates that a parsed classification event on the event stream was originated from a different
     * asset classification smart contract than the one configured to be watched by the VerifierClient.  This will be
     * very common in an environment that includes multiple contract instances and can be safely ignored.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredContractMismatch : VerifierEventType<VerifierEvent.EventIgnoredContractMismatch>

    /**
     * Only handle events that are relevant to the verifier.  The asset classification smart contract emits many more
     * events than the verifier client needs to handle, and this event is triggered when one of those events occur.
     * This can be safely ignored, but is available for debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredUnhandledEventType : VerifierEventType<VerifierEvent.EventIgnoredUnhandledEventType>

    /**
     * This should never happen.  It indicates a change was made to the asset classification smart contract and/or the
     * underlying event processing that allowed an event that was not a handled type (ACContractEvent.HANDLED_EVENTS)
     * to pass into the event type when switch.  This event should be considered an error case and any external error
     * handling should be used to alert of this error.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredUnknownEvent : VerifierEventType<VerifierEvent.EventIgnoredUnknownEvent>

    /**
     * This event is emitted when the verifier client picks up an event that has already completed being verified.  This
     * should only happen when events are re-run by crawling old data in the event stream.  This can safely be ignored
     * unless interactions need to occur when encountering a completed event.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param message A message indicating the nature of the event.
     */
    object OnboardEventIgnoredPreviouslyProcessed : VerifierEventType<VerifierEvent.OnboardEventIgnoredPreviouslyProcessed>

    /**
     * This event is emitted when the VerificationProcessor's defined method of fetching an asset from a scope attribute's
     * data fails and throws an exception.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param t The exception thrown when attempting to use the [VerificationProcessor] to fetch an asset.
     */
    object OnboardEventFailedToRetrieveAsset : VerifierEventType<VerifierEvent.OnboardEventFailedToRetrieveAsset>

    /**
     * This event is emitted when the VerificationProcessor's defined method of verifying asset data from a fetched
     * asset fails and throws an exception.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param t The exception thrown when attempting to use the [VerificationProcessor] to get verification details.
     */
    object OnboardEventFailedToVerifyAsset : VerifierEventType<VerifierEvent.OnboardEventFailedToVerifyAsset>

    /**
     * This event always triggers before verification a verification transaction is sent to the smart contract for a
     * scope.  This can safely be ignored unless a pre verification hook is needed.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     */
    object OnboardEventPreVerifySend : VerifierEventType<VerifierEvent.OnboardEventPreVerifySend>

    /**
     * This event is emitted when an asset classification smart contract "verify asset" event is detected and its scope
     * attribute has been fetched.  This is an error case that indicates that although a verification event was run,
     * the resulting scope attribute is now in an unexpected state.  This can theoretically happen if verification has been
     * denied and then onboarding has been once again invoked, but the likelihood of that occurrence versus bad data
     * is low.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param message A message indicating the nature of the event.
     */
    object VerifyEventFailedUnexpectedOnboardingStatus : VerifierEventType<VerifierEvent.VerifyEventUnexpectedOnboardingStatus>

    /**
     * This event is emitted when an asset classification smart contract "verify asset" event is processed and has
     * successfully moved the scope attribute's status to either approved or denied.  This event should be considered
     * the ideal ending process for all scopes that pass through the asset classification smart contract.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param newOnboardingStatus The new status that the asset has been moved to after successful onboarding.
     */
    object VerifyEventSuccessful : VerifierEventType<VerifierEvent.VerifyEventSuccessful>

    /**
     * This event is emitted when an exception occurs attempting to send an asset verification to the smart contract
     * with the ACClient instance.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     * @param message A message indicating the nature of the event.
     * @param t The exception thrown.
     */
    object VerifyAssetSendThrewException : VerifierEventType<VerifierEvent.VerifyAssetSendThrewException>

    /**
     * This event is emitted when the verifier attempts to re-synchronize its sequence number with the registered
     * verifier account's values on the blockchain, and that process throws an exception.  This indicates that blockchain
     * communication is currently failing and that the verifier will be unable to successfully submit transactions.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     * @param message A message indicating the nature of the event.
     * @param t The exception thrown when attempting to query account information for the registered verifier address.
     */
    object VerifyAssetSendSyncSequenceNumberFailed : VerifierEventType<VerifierEvent.VerifyAssetSendSyncSequenceNumberFailed>

    /**
     * This event is emitted when the asset classification smart contract receives and processes a verification.  The
     * "verify asset" event processing must still collect the event and verify the status of the asset scope attribute
     * before considering the verification process to be complete for the target asset, but this event at least
     * signifies that the transaction to verify the asset was processed.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     */
    object VerifyAssetSendSucceeded : VerifierEventType<VerifierEvent.VerifyAssetSendSucceeded>

    /**
     * This event is emitted when attempting to verify the asset in a transaction fails.  The response includes the
     * information returned via the Provenance Blockchain about the problem.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param scopeAttribute All details related to the asset, memorialized in an attribute attached to the asset's
     * Provenance Blockchain Metadata Scope.
     * @param verification The verification details retrieved from using the [VerificationProcessor] to retrieve an
     * asset.
     * @param responseCode The response code received from the Provenance Blockchain.  A zero indicates a success, so
     * this value is guaranteed to be non-zero when this event is encountered.
     * @param rawLog The log text received from the Provenance Blockchain, generally indicating the nature of the
     * error encountered that caused the verification message to not be processed.
     */
    object VerifyAssetSendFailed : VerifierEventType<VerifierEvent.VerifyAssetSendFailed>

    /**
     * This event is emitted when the coroutine channel used to process verifier events sequentially throws an exception.
     * Any number of coroutine/async things can occur that can cause this error when the environment is having difficulties
     * and it should be watched in a robust setup.
     *
     * @param t The exception thrown.
     */
    object VerifyEventChannelThrewException : VerifierEventType<VerifierEvent.VerifyEventChannelThrewException>

    /**
     * This will commonly happen - the contract emits events that don't target the verifier at all, but they'll
     * still pass through here.  This can be safely ignored, but is available for debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredNoVerifierAddress : VerifierEventType<VerifierEvent.EventIgnoredNoVerifierAddress>

    /**
     * Only process verifications that are targeted at the registered verifier account.  Other verifiers can be chosen,
     * and those events will still pass through the verifier client.  This can be safely ignored, but is available for
     * debugging.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param registeredVerifierAddress The address of the verifier that is currently held by the client.  This is
     * used to make the message produced more contextually relevant.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredDifferentVerifierAddress : VerifierEventType<VerifierEvent.EventIgnoredDifferentVerifierAddress>

    /**
     * This event is emitted when an asset classification smart contract event is detected, but it does not contain the
     * expected scope address attribute.  This is an error case and indicates bad smart contract code.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredMissingScopeAddress : VerifierEventType<VerifierEvent.EventIgnoredMissingScopeAddress>

    /**
     * This event is emitted when an asset classification smart contract event is detected, but it does not contain the
     * expected asset type for an onboarded scope.  This is an error case and indicates bad smart contract code.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The asset classification event type emitted that was missing its scope address.
     * @param message A message indicating the nature of the event.
     */
    object EventIgnoredMissingAssetType : VerifierEventType<VerifierEvent.EventIgnoredMissingAssetType>

    /**
     * This event is emitted when an asset classification smart contract event is detected, but the scope
     * that was onboarded does not yet have an AssetScopeAttribute on it.  This should only ever occur due to a
     * blockchain error, or indicates a smart contract bug.
     *
     * @param event All values from the encountered event that match the event attribute structure emitted by the
     * asset classification smart contract.
     * @param eventType The type of smart contract event that was encountered when the scope attribute was missing.
     * @param message A message indicating the nature of the event.
     * @param t The exception thrown when attempting to fetch the scope attribute from the smart contract.  Indicates
     * the reason for the missing attribute.
     */
    object EventIgnoredMissingScopeAttribute : VerifierEventType<VerifierEvent.EventIgnoredMissingScopeAttribute>

    /**
     * This event is emitted when an event processor is registered in the VerifierClientConfig that can throw an exception.
     * The VerifierClient will catch any exception thrown by custom processor code and emit this error with the
     * exception thrown contained as `t`.  It is HIGHLY recommended that only safe code be run in the handler for this
     * event, because if the code in the handler fails with an exception, the problem is silently ignored.
     *
     * @param failedEventName The name of the [VerifierEvent] that had a misconfigured custom event process that threw
     * an exception.
     * @param t The exception thrown during the event processor's invocation.
     */
    object EventProcessorFailed : VerifierEventType<VerifierEvent.EventProcessorFailed>

    /**
     * This event allows an external consumer of this library to create their own event handlers with as many custom
     * event types as they want.  It behaves differently in the event processing code, keying off of the [eventName]
     * property, versus keying off of its class name.
     *
     * @param eventName The unique event type identifier key.  This value should be different for each custom event.
     * It is recommended that an enum be used for each different custom event to ensure uniqueness, but this is entirely
     * up to the consumer of the library to decide.
     * @param eventBody Any T type that needs to be emitted in the event.  This will likely be a data class containing
     * many additional fields.
     */
    class CustomEvent<T>(private val eventName: String) : VerifierEventType<VerifierEvent.CustomEvent<T>> {
        override fun getEventTypeName(): String = eventName
    }
}
