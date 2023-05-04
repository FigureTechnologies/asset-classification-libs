package tech.figure.classification.asset.verifier.event.defaults

import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredDifferentVerifierAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingAssetType
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredNoVerifierAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventSuccessful
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventUnexpectedOnboardingStatus
import tech.figure.classification.asset.verifier.event.AssetClassificationEventHandler
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import tech.figure.classification.asset.verifier.provenance.ACContractEvent

object DefaultVerifyAssetEventHandler : AssetClassificationEventHandler {
    private val expectedOnboardingStatuses: List<AssetOnboardingStatus> = listOf(
        AssetOnboardingStatus.APPROVED,
        AssetOnboardingStatus.DENIED
    )

    override val eventType: ACContractEvent = ACContractEvent.VERIFY_ASSET

    override suspend fun handleEvent(parameters: EventHandlerParameters) {
        val (event, _, verifierAccount, _, _, eventChannel) = parameters
        val messagePrefix = "[VERIFY ASSET | Tx: ${event.sourceEvent.txHash} | Asset ${event.scopeAddress} / ${event.assetType}"
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
        // Verify event construction integrity to ensure that contract anomalies did not occur
        event.scopeAddress ?: run {
            eventChannel.send(
                EventIgnoredMissingScopeAddress(
                    event = event,
                    eventType = this.eventType,
                    message = "$messagePrefix Expected the verify asset event to include a scope address, but it was missing"
                )
            )
            return
        }
        event.assetType ?: run {
            eventChannel.send(
                EventIgnoredMissingAssetType(
                    event = event,
                    eventType = this.eventType,
                    message = "$messagePrefix Expected the verify asset event to include an asset type, but it was missing"
                )
            )
            return
        }
        val onboardingStatus = event.assetOnboardingStatus
            ?.takeIf { it in expectedOnboardingStatuses }
            ?: run {
                eventChannel.send(
                    VerifyEventUnexpectedOnboardingStatus(
                        event = event,
                        message = "$messagePrefix Verification produced an unexpected onboarding status of [${event.assetOnboardingStatus?.contractName}]"
                    )
                )
                return
            }
        eventChannel.send(VerifyEventSuccessful(event, onboardingStatus))
    }
}
