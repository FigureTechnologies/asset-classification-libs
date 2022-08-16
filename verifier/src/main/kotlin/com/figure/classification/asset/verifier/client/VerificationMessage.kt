package com.figure.classification.asset.verifier.client

import com.figure.classification.asset.client.domain.model.AssetScopeAttribute
import com.figure.classification.asset.verifier.provenance.AssetClassificationEvent

/**
 * A message sent to the VerifierClient's verification channel for processing.  Used in event handlers.
 *
 * @param failureMessagePrefix A prefix to append to events emitted by the verification channel processor.
 * @param event The event from the smart contract that caused the verification to be processed.
 * @param scopeAttribute The attribute attached to the scope being verified.
 * @param verification An indication of if the verification was successful or not.
 */
data class VerificationMessage(
    val failureMessagePrefix: String,
    val event: AssetClassificationEvent,
    val scopeAttribute: AssetScopeAttribute,
    val verification: AssetVerification,
)
