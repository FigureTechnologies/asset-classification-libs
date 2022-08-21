package tech.figure.classification.asset.verifier.event.defaults

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import tech.figure.classification.asset.verifier.client.AssetVerification
import tech.figure.classification.asset.verifier.config.VerifierEvent
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAddress
import tech.figure.classification.asset.verifier.config.VerifierEvent.EventIgnoredMissingScopeAttribute
import tech.figure.classification.asset.verifier.config.VerifierEvent.OnboardEventPreVerifySend
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import tech.figure.classification.asset.verifier.provenance.ACContractEvent
import tech.figure.classification.asset.verifier.testhelpers.MockACAttribute
import tech.figure.classification.asset.verifier.testhelpers.MockTxEvent
import tech.figure.classification.asset.verifier.testhelpers.MockTxEvent.MockTxEventBuilder
import tech.figure.classification.asset.verifier.testhelpers.assertLastEvent
import tech.figure.classification.asset.verifier.testhelpers.getMockAccountDetail
import tech.figure.classification.asset.verifier.testhelpers.getMockScopeAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DefaultOnboardEventHandlerTest {
    @Test
    fun `test no verifier address included in event`() = runTest {
        val parameters = getMockParameters(includeVerifierAddress = false)
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<VerifierEvent.EventIgnoredNoVerifierAddress>(parameters) { (event, eventType) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.ONBOARD_ASSET,
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
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<VerifierEvent.EventIgnoredDifferentVerifierAddress>(parameters) { (event, eventType, registeredVerifierAddress) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.ONBOARD_ASSET,
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
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<EventIgnoredMissingScopeAddress>(parameters) { (event, eventType, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to contain the asset classification event",
            )
            assertEquals(
                expected = ACContractEvent.ONBOARD_ASSET,
                actual = eventType,
                message = "Expected the event type to be an onboard asset event",
            )
            assertTrue(
                actual = "Expected the onboard asset event to include a scope address, but it was missing" in message,
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
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<EventIgnoredMissingScopeAttribute>(parameters) { (event, eventType, message, t) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to be included in the error",
            )
            assertEquals(
                expected = ACContractEvent.ONBOARD_ASSET,
                actual = eventType,
                message = "Expected the event type to be an onboard asset event",
            )
            assertTrue(
                actual = "Intercepted onboard asset did not point to a scope with a scope attribute" in message,
                message = "Expected the event message to be formatted correctly",
            )
            assertTrue(
                actual = t is IllegalStateException,
                message = "Expected the throwable to be formatted correctly",
            )
            assertEquals(
                expected = "Failed to query for scope",
                actual = t.message,
                message = "Expected the error message to be populated correctly",
            )
        }
    }

    @Test
    fun `test invalid AssetOnboardingStatus in event`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        val mockScopeAttribute = getMockScopeAttribute(onboardingStatus = AssetOnboardingStatus.DENIED)
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } returns mockScopeAttribute
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<VerifierEvent.OnboardEventIgnoredPreviouslyProcessed>(parameters) { (event, scopeAttribute, message) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the classification event to be included in the event",
            )
            assertEquals(
                expected = scopeAttribute,
                actual = mockScopeAttribute,
                message = "Expected the scope attribute to be included in the event",
            )
            assertTrue(
                actual = "Scope attribute indicates an onboarding status of [DENIED], which is not actionable. Has verification: [Verified = false | Message = MOCK: Denied]" in message,
                message = "Expected the proper message to be included in the event, but got message: $message",
            )
        }
    }

    @Test
    fun `test failure in retrieveAsset`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        val mockScopeAttribute = getMockScopeAttribute()
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } returns mockScopeAttribute
        coEvery { parameters.processor.retrieveAsset(any(), any(), any()) } throws IllegalStateException("MOCK: Failed to retrieve asset")
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<VerifierEvent.OnboardEventFailedToRetrieveAsset>(parameters) { (event, scopeAttribute, t) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to include the classification event from the parameters",
            )
            assertEquals(
                expected = mockScopeAttribute,
                actual = scopeAttribute,
                message = "Expected the event to include the retrieved scope attribute",
            )
            assertTrue(
                actual = t is IllegalStateException,
                message = "Expected the exception to be an IllegalStateException, but was ${t::class.simpleName}",
            )
            assertEquals(
                expected = "MOCK: Failed to retrieve asset",
                actual = t.message,
                message = "Expected the exception to include the correct message",
            )
        }
    }

    @Test
    fun `test failure in verifyAsset`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        val mockScopeAttribute = getMockScopeAttribute()
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } returns mockScopeAttribute
        coEvery { parameters.processor.retrieveAsset(any(), any(), any()) } returns "MOCK ASSET"
        coEvery { parameters.processor.verifyAsset(any(), any(), any()) } throws IllegalStateException("MOCK: Failed to verify asset")
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "The verification channel should not receive any input",
        )
        assertLastEvent<VerifierEvent.OnboardEventFailedToVerifyAsset>(parameters) { (event, scopeAttribute, t) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to include the classification event from the parameters",
            )
            assertEquals(
                expected = mockScopeAttribute,
                actual = scopeAttribute,
                message = "Expected the event to include the retrieved scope attribute",
            )
            assertTrue(
                actual = t is IllegalStateException,
                message = "Expected the exception to be an IllegalStateException, but was ${t::class.simpleName}",
            )
            assertEquals(
                expected = "MOCK: Failed to verify asset",
                actual = t.message,
                message = "Expected the exception to include the correct message",
            )
        }
    }

    @Test
    fun `test successful event processing`() = runTest {
        val parameters = getMockParameters { builder ->
            builder
                .addACAttribute(MockACAttribute.ScopeAddress("mock-scope-address"))
        }
        val mockScopeAttribute = getMockScopeAttribute()
        val mockVerification = AssetVerification(
            message = "MOCK: Successful verification",
            success = true,
        )
        every { parameters.acClient.queryAssetScopeAttributeByScopeAddress(any()) } returns mockScopeAttribute
        coEvery { parameters.processor.retrieveAsset(any(), any(), any()) } returns "MOCK ASSET"
        coEvery { parameters.processor.verifyAsset(any(), any(), any()) } returns mockVerification
        DefaultOnboardEventHandler.handleEvent(parameters)
        assertLastEvent<OnboardEventPreVerifySend>(parameters) { (event, scopeAttribute, verification) ->
            assertEquals(
                expected = parameters.event,
                actual = event,
                message = "Expected the event to include the classification event from the parameters",
            )
            assertEquals(
                expected = mockScopeAttribute,
                actual = scopeAttribute,
                message = "Expected the event to include the retrieved scope attribute",
            )
            assertEquals(
                expected = mockVerification,
                actual = verification,
                message = "Expected the verification derived from the processor to be included in the event",
            )
        }
        assertFalse(
            actual = parameters.verificationChannel.isEmpty,
            message = "A verification channel event should be sent",
        )
        val message = parameters.verificationChannel.receive()
        assertEquals(
            expected = parameters.event,
            actual = message.event,
            message = "The classification event should be included in the verification message",
        )
        assertEquals(
            expected = mockScopeAttribute,
            actual = message.scopeAttribute,
            message = "The scope attribute should be included in the verification message",
        )
        assertEquals(
            expected = mockVerification,
            actual = message.verification,
            message = "The verification should be included in the verification message",
        )
        assertTrue(
            actual = parameters.verificationChannel.isEmpty,
            message = "There should be no more verification channel messages",
        )
    }

    private fun getMockParameters(
        verifierAccount: ProvenanceAccountDetail = getMockAccountDetail(),
        includeVerifierAddress: Boolean = true,
        builderFn: (MockTxEventBuilder) -> MockTxEventBuilder = { it },
    ): EventHandlerParameters = MockTxEvent
        .builder()
        .addACAttribute(MockACAttribute.EventType(ACContractEvent.ONBOARD_ASSET))
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
}
