package com.figure.classification.asset.verifier.event

import com.figure.classification.asset.client.client.base.ACClient
import com.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import com.figure.classification.asset.verifier.client.VerificationMessage
import com.figure.classification.asset.verifier.config.VerificationProcessor
import com.figure.classification.asset.verifier.config.VerifierEvent
import com.figure.classification.asset.verifier.provenance.ACContractEvent
import com.figure.classification.asset.verifier.provenance.AssetClassificationEvent
import kotlinx.coroutines.channels.Channel

/**
 * A simple interface defining the structure for an event handler. These values are consumed and delegated to by the
 * AssetClassificationEventDelegator.
 *
 * @property eventType The event type related to the handler.  Each variant of ACContractEvent can only be registered
 * once in an AssetClassificationEventDelegator.
 */
interface AssetClassificationEventHandler {
    val eventType: ACContractEvent

    /**
     * Takes an instance of EventHandlerParameters and processes the contained AssetClassificationEvent, taking action
     * where necessary.
     */
    suspend fun handleEvent(parameters: EventHandlerParameters)
}

/**
 * Controls all values required for the AssetClassificationEventHandler to perform all necessary actions.
 *
 * @param event The processed event from the asset classification smart contract, indicating that an event was emitted.
 * @param acClient A client instance used to communicate with the current environment's asset classification smart contract instance.
 * @param verifierAccount The account details for the verifier address.  The asset classification should emit events that
 * include a bech32 address for an account designated as a "verifier," and this account should be used to ensure that
 * processed events link to the correct entity.
 * @param processor A VerificationProcessor implementation that tells the VerifierClient how to retrieve asset data
 * based on an event and how to determine the authenticity of that asset, when retrieved.
 * @param verificationChannel The coroutine channel that processes VerificationMessage objects, which are all the
 * values needed to send a verification message to the smart contract.  This channel should be used when a verification
 * needs to be triggered in the VerifierClient.
 * @param eventChannel The coroutine channel that processes all VerifierEvent values emitted in the event handlers.
 * This should be used as often as is necessary to alert the downstream consumer of the VerifierClient's events.
 * Also: See VerifierEvent.CustomEvent for custom implementations.
 */
data class EventHandlerParameters(
    val event: AssetClassificationEvent,
    val acClient: ACClient,
    val verifierAccount: ProvenanceAccountDetail,
    val processor: VerificationProcessor<Any>,
    val verificationChannel: Channel<com.figure.classification.asset.verifier.client.VerificationMessage>,
    val eventChannel: Channel<VerifierEvent>,
)
