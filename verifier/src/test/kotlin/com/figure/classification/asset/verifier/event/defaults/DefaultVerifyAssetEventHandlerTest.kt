package com.figure.classification.asset.verifier.event.defaults

import com.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import com.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import com.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredDifferentVerifierAddress
import com.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAddress
import com.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAttribute
import com.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredNoVerifierAddress
import com.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventFailedOnboardingStatusStillPending
import com.figure.classification.asset.verifier.config.VerifierEvent.VerifyEventSuccessful
import com.figure.classification.asset.verifier.event.EventHandlerParameters
import com.figure.classification.asset.verifier.provenance.ACContractEvent
import com.figure.classification.asset.verifier.testhelpers.MockACAttribute
import com.figure.classification.asset.verifier.testhelpers.MockTxEvent
import com.figure.classification.asset.verifier.testhelpers.MockTxEvent.MockTxEventBuilder
import com.figure.classification.asset.verifier.testhelpers.assertLastEvent
import com.figure.classification.asset.verifier.testhelpers.getMockAccountDetail
import com.figure.classification.asset.verifier.testhelpers.getMockScopeAttribute
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `test no scope attribute included in event`() = runTest {
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
    fun `test failure to find scope attribute`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } throws IllegalStateException("Failed to query for scope")
        parameters.handleEvent()
        assertLastEvent<EventIgnoredMissingScopeAttribute>(parameters) { (event, eventType, message, t) ->
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
                actual = "Intercepted verification did not point to a scope with a scope attribute" in message,
                message = "Expected the correct event message to be included in the output",
            )
            assertTrue(
                actual = t is IllegalStateException,
                message = "Expected the correct exception to be encountered",
            )
            assertEquals(
                expected = "Failed to query for scope",
                actual = t.message,
                message = "Expected the correct exception text to be encountered",
            )
        }
    }

    @Test
    fun `test failure due to onboarding status still pending`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        val mockScopeAttribute = getMockScopeAttribute(onboardingStatus = AssetOnboardingStatus.PENDING)
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } returns mockScopeAttribute
        parameters.handleEvent()
        assertLastEvent<VerifyEventFailedOnboardingStatusStillPending>(parameters) { (event, scopeAttribute, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = mockScopeAttribute,
                actual = scopeAttribute,
                message = "Expected the event to contain the scope attribute",
            )
            assertTrue(
                actual = "Verification did not successfully move onboarding status from pending" in message,
                message = "Expected the correct event message to be included in the output",
            )
        }
    }

    @Test
    fun `test successful event processing`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        val mockScopeAttribute = getMockScopeAttribute(onboardingStatus = AssetOnboardingStatus.APPROVED)
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } returns mockScopeAttribute
        parameters.handleEvent()
        assertLastEvent<VerifyEventSuccessful>(parameters) { (event, scopeAttribute) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = mockScopeAttribute,
                actual = scopeAttribute,
                message = "Expected the event to contain the scope attribute",
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
