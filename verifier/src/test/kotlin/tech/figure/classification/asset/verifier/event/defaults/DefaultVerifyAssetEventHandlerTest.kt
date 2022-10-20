package tech.figure.classification.asset.verifier.event.defaults

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredDifferentVerifierAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingAssetType
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredNoVerifierAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventSuccessful
import tech.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventUnexpectedOnboardingStatus
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import tech.figure.classification.asset.verifier.provenance.ACContractEvent
import tech.figure.classification.asset.verifier.testhelpers.MockACAttribute
import tech.figure.classification.asset.verifier.testhelpers.MockTxEvent
import tech.figure.classification.asset.verifier.testhelpers.MockTxEvent.MockTxEventBuilder
import tech.figure.classification.asset.verifier.testhelpers.assertLastEvent
import tech.figure.classification.asset.verifier.testhelpers.getMockAccountDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DefaultVerifyAssetEventHandlerTest {
    @Test
    fun `test no verifier address included in event`() = runTest {
        val parameters = getMockParameters(includeVerifierAddress = false)
        parameters.handleEvent()
        assertLastEvent<EventIgnoredNoVerifierAddress>(parameters) { (event, eventType) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.VERIFY_ASSET,
                actual = eventType,
                message = "Expected the event type to be a verify asset event",
            )
        }
    }

    @Test
    fun `test different verifier address in event`() = runTest {
        val parameters = getMockParameters(includeVerifierAddress = false) { builder ->
            builder.addACAttribute(MockACAttribute.VerifierAddress("wrong-address"))
        }
        parameters.handleEvent()
        assertLastEvent<EventIgnoredDifferentVerifierAddress>(parameters) { (event, eventType, registeredVerifierAddress) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.VERIFY_ASSET,
                actual = eventType,
                message = "Expected the event type to be a verify asset event",
            )
            assertEquals(
                expected = parameters.verifierAccount.bech32Address,
                actual = registeredVerifierAddress,
                message = "Expected the verifier address to be properly emitted in the event",
            )
        }
    }

    @Test
    fun `test no scope address attribute included in event`() = runTest {
        val parameters = getMockParameters()
        parameters.handleEvent()
        assertLastEvent<EventIgnoredMissingScopeAddress>(parameters) { (event, eventType, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.VERIFY_ASSET,
                actual = eventType,
                message = "Expected the event type to be a verify asset event",
            )
            assertTrue(
                actual = "Expected the verify asset event to include a scope address, but it was missing" in message,
                message = "Expected the correct event message to be included in the output",
            )
        }
    }

    @Test
    fun `test no asset type attribute included in event`() = runTest {
        val parameters = getMockParameters { builder ->
            builder.addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        parameters.handleEvent()
        assertLastEvent<EventIgnoredMissingAssetType>(parameters) { (event, eventType, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.VERIFY_ASSET,
                actual = eventType,
                message = "Expected the event type to be a verify asset event",
            )
            assertTrue(
                actual = "Expected the verify asset event to include an asset type, but it was missing" in message,
                message = "Expected the correct event message to be included in the output",
            )
        }
    }

    @Test
    fun `test failure due to null onboarding status`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
                .addACAttribute(MockACAttribute.AssetType("mock-asset-type"))
        }
        parameters.handleEvent()
        assertLastEvent<VerifyEventUnexpectedOnboardingStatus>(parameters) { (event, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertNull(
                actual = event.assetOnboardingStatus,
                message = "Expected the event to be missing an onboarding status",
            )
            assertTrue(
                actual = "Verification produced an unexpected onboarding status of [null]" in message,
                message = "Expected the correct event message to be included in the output",
            )
        }
    }

    @Test
    fun `test failure due to onboarding status still pending`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
                .addACAttribute(MockACAttribute.AssetType("mock-asset-type"))
                .addACAttribute(MockACAttribute.OnboardingStatus(AssetOnboardingStatus.PENDING))
        }
        parameters.handleEvent()
        assertLastEvent<VerifyEventUnexpectedOnboardingStatus>(parameters) { (event, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = event.assetOnboardingStatus,
                actual = AssetOnboardingStatus.PENDING,
                message = "Expected the event to contain the onboarding status that caused the issue",
            )
            assertTrue(
                actual = "Verification produced an unexpected onboarding status of [${AssetOnboardingStatus.PENDING.contractName}]" in message,
                message = "Expected the correct event message to be included in the output",
            )
        }
    }

    @Test
    fun `test successful event processing`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
                .addACAttribute(MockACAttribute.AssetType("mock-asset-type"))
                .addACAttribute(MockACAttribute.OnboardingStatus(AssetOnboardingStatus.APPROVED))
        }
        parameters.handleEvent()
        assertLastEvent<VerifyEventSuccessful>(parameters) { (event, onboardingStatus) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = AssetOnboardingStatus.APPROVED,
                actual = onboardingStatus,
                message = "Expected the event to contain the scope proper onboarding status",
            )
        }
    }

    private fun getMockParameters(
        verifierAccount: ProvenanceAccountDetail = getMockAccountDetail(),
        includeVerifierAddress: Boolean = true,
        builderFn: (MockTxEventBuilder) -> MockTxEventBuilder = { it },
    ): EventHandlerParameters = MockTxEvent
        .builder()
        .addACAttribute(MockACAttribute.EventType(ACContractEvent.VERIFY_ASSET))
        .apply {
            if (includeVerifierAddress) {
                addACAttribute(MockACAttribute.VerifierAddress(verifierAccount.bech32Address))
            }
        }
        .let(builderFn)
        .buildACEvent()
        .let { event ->
            EventHandlerParameters(
                event = event,
                acClient = mockk(),
                verifierAccount = verifierAccount,
                processor = mockk(),
                verificationChannel = Channel(capacity = Channel.BUFFERED),
                eventChannel = Channel(capacity = Channel.BUFFERED),
            )
        }

    private suspend fun EventHandlerParameters.handleEvent() {
        DefaultVerifyAssetEventHandler.handleEvent(this)
        assertTrue(
            actual = this.verificationChannel.isEmpty,
            message = "The verification channel should never receive input from this event type",
        )
    }
}
