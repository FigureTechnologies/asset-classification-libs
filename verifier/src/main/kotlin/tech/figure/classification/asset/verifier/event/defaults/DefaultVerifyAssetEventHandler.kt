package tech.figure.classification.asset.verifier.event.defaults

import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredDifferentVerifierAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAttribute
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredNoVerifierAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventFailedOnboardingStatusStillPending
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventSuccessful
import tech.figure.classification.asset.verifier.event.AssetClassificationEventHandler
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import tech.figure.classification.asset.verifier.provenance.ACContractEvent

object DefaultVerifyAssetEventHandler : AssetClassificationEventHandler {
    override val eventType: ACContractEvent = ACContractEvent.VERIFY_ASSET

    override suspend fun handleEvent(parameters: EventHandlerParameters) {
        val (event, acClient, verifierAccount, _, _, eventChannel) = parameters
        val messagePrefix = "[VERIFY ASSET | Tx: ${event.sourceEvent.txHash} | Asset ${event.scopeAddress}"
        // This will commonly happen - the contract emits events that don't target the verifier at all, but they'll
        // still pass through here
        if (event.verifierAddress == null) {
            eventChannel.send(EventIgnoredNoVerifierAddress(event, this.eventType))
            return
        }
        // Only process verifications that are targeted at the registered verifier account
        if (event.verifierAddress != parameters.verifierAccount.bech32Address) {
            eventChannel.send(
                EventIgnoredDifferentVerifierAddress(
                    event = event,
                    eventType = this.eventType,
                    registeredVerifierAddress = verifierAccount.bech32Address
                )
            )
            return
        }
        val scopeAddress = event.scopeAddress ?: run {
            eventChannel.send(
                EventIgnoredMissingScopeAddress(
                    event = event,
                    eventType = this.eventType,
                    message = "$messagePrefix Expected the verify asset event to include a scope address, but it was missing",
                )
            )
            return
        }
        val scopeAttribute = try {
            acClient.queryAssetScopeAttributeByScopeAddress(scopeAddress)
        } catch (t: Throwable) {
            eventChannel.send(
                EventIgnoredMissingScopeAttribute(
                    event = event,
                    eventType = this.eventType,
                    message = "$messagePrefix Intercepted verification did not point to a scope with a scope attribute",
                    t = t,
                )
            )
            return
        }
        if (scopeAttribute.onboardingStatus == AssetOnboardingStatus.PENDING) {
            eventChannel.send(
                VerifyEventFailedOnboardingStatusStillPending(
                    event = event,
                    scopeAttribute = scopeAttribute,
                    message = "$messagePrefix Verification did not successfully move onboarding status from pending",
                )
            )
            return
        }
        eventChannel.send(VerifyEventSuccessful(event, scopeAttribute))
    }
}
