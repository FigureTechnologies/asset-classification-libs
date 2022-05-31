package io.provenance.classification.asset.verifier.event.defaults

import io.provenance.classification.asset.client.domain.model.AccessDefinitionType
import io.provenance.classification.asset.client.domain.model.AssetOnboardingStatus
import io.provenance.classification.asset.verifier.client.VerificationMessage
import io.provenance.classification.asset.verifier.config.VerifierEvent.EventIgnoredDifferentVerifierAddress
import io.provenance.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAddress
import io.provenance.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAttribute
import io.provenance.classification.asset.verifier.config.VerifierEvent.EventIgnoredNoVerifierAddress
import io.provenance.classification.asset.verifier.config.VerifierEvent.OnboardEventFailedToRetrieveAsset
import io.provenance.classification.asset.verifier.config.VerifierEvent.OnboardEventFailedToVerifyAsset
import io.provenance.classification.asset.verifier.config.VerifierEvent.OnboardEventIgnoredPreviouslyProcessed
import io.provenance.classification.asset.verifier.config.VerifierEvent.OnboardEventPreVerifySend
import io.provenance.classification.asset.verifier.event.AssetClassificationEventHandler
import io.provenance.classification.asset.verifier.event.EventHandlerParameters
import io.provenance.classification.asset.verifier.provenance.ACContractEvent

object DefaultOnboardEventHandler : AssetClassificationEventHandler {
    override val eventType: ACContractEvent = ACContractEvent.ONBOARD_ASSET

    override suspend fun handleEvent(parameters: EventHandlerParameters) {
        val (event, acClient, verifierAccount, processor, verificationChannel, eventChannel) = parameters
        val messagePrefix = "[ONBOARD_ASSET | Tx: ${event.sourceEvent.txHash} | Asset: ${event.scopeAddress}]:"
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
                    message = "$messagePrefix Expected the onboard asset event to include a scope address, but it was missing",
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
                    message = "$messagePrefix Intercepted onboard asset did not point to a scope with a scope attribute",
                    t = t,
                )
            )
            return
        }
        if (scopeAttribute.onboardingStatus != AssetOnboardingStatus.PENDING) {
            eventChannel.send(
                OnboardEventIgnoredPreviouslyProcessed(
                    event = event,
                    scopeAttribute = scopeAttribute,
                    message = "$messagePrefix Scope attribute indicates an onboarding status of [${scopeAttribute.onboardingStatus}], which is not actionable. Has verification: [Verified = ${scopeAttribute.latestVerificationResult?.success} | Message = ${scopeAttribute.latestVerificationResult?.message}]",
                )
            )
            return
        }
        val targetRoutes = scopeAttribute.accessDefinitions.singleOrNull { it.definitionType == AccessDefinitionType.REQUESTOR }
            ?.accessRoutes
            // Provide an empty list if no access routes were defined by the requestor
            ?: emptyList()
        val asset = try {
            processor.retrieveAsset(event, scopeAttribute, targetRoutes)
        } catch (t: Throwable) {
            eventChannel.send(
                OnboardEventFailedToRetrieveAsset(
                    event = event,
                    scopeAttribute = scopeAttribute,
                    t = t,
                )
            )
            null
        } ?: return
        val verification = try {
            processor.verifyAsset(event, scopeAttribute, asset)
        } catch (t: Throwable) {
            eventChannel.send(
                OnboardEventFailedToVerifyAsset(
                    event = event,
                    scopeAttribute = scopeAttribute,
                    t = t,
                )
            )
            null
        } ?: return
        eventChannel.send(OnboardEventPreVerifySend(event, scopeAttribute, verification))
        verificationChannel.send(
            VerificationMessage(
                failureMessagePrefix = messagePrefix,
                event = event,
                scopeAttribute = scopeAttribute,
                verification = verification,
            )
        )
    }
}
