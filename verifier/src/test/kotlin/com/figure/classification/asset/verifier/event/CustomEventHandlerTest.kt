package com.figure.classification.asset.verifier.event

import com.figure.classification.asset.util.enums.ProvenanceNetworkType
import com.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import com.figure.classification.asset.verifier.client.VerifierClient
import com.figure.classification.asset.verifier.config.VerifierClientConfig
import com.figure.classification.asset.verifier.config.VerifierCoroutineScopeConfig
import com.figure.classification.asset.verifier.config.VerifierEvent
import com.figure.classification.asset.verifier.config.VerifierEventType.CustomEvent
import com.figure.classification.asset.verifier.event.InstantiateContractEventHandler.CUSTOM_EVENT_NAME
import com.figure.classification.asset.verifier.provenance.ACContractEvent
import com.figure.classification.asset.verifier.testhelpers.MockACAttribute
import com.figure.classification.asset.verifier.testhelpers.MockTxEvent
import cosmos.auth.v1beta1.Auth
import cosmos.auth.v1beta1.QueryGrpc.QueryBlockingStub
import cosmos.auth.v1beta1.QueryOuterClass
import io.mockk.every
import io.mockk.mockk
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.hdwallet.bip39.MnemonicWords
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.BeforeEach
import java.security.Security
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class CustomEventHandlerTest {
    @BeforeEach
    fun beforeTest() {
        // Register the bouncycastle provider to ensure that key derivation in tests does not error when BC resources
        // are requested
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun `test custom event handling`() = runTest {
        val capturedMetadata = mutableListOf<String>()
        val testContext = TestCoroutineScheduler()
        val config = VerifierClientConfig.builder(
            acClient = mockk(),
            // The client tries to derive the signer immediately, so a "real" account detail must be provided
            verifierAccount = ProvenanceAccountDetail.fromMnemonic(
                mnemonic = MnemonicWords.generate().toString(),
                networkType = ProvenanceNetworkType.TESTNET,
            ),
            verificationProcessor = mockk(),
        ).withEventDelegator(
            AssetClassificationEventDelegator.defaultBuilder()
                .registerEventHandler(InstantiateContractEventHandler)
                .build()
        ).addEventProcessor(CustomEvent<InstantiateInfo>(CUSTOM_EVENT_NAME)) { event ->
            capturedMetadata += event.eventBody.data
        }.withCoroutineScope(VerifierCoroutineScopeConfig.ProvidedScope(TestScope(context = testContext))).build()
        config.registerMocks()
        val client = VerifierClient(config)
        client.startEventChannelReceiver()
        // Send an instantiate event that should be routed into the custom handler for that type
        client.handleEvent(
            event = MockTxEvent
                .builder()
                .addACAttribute(MockACAttribute.EventType(ACContractEvent.INSTANTIATE_CONTRACT))
                .addACAttribute(MockACAttribute.VerifierAddress(config.verifierAccount.bech32Address))
                .addACAttribute(MockACAttribute.AdditionalMetadata("expected value"))
                .buildACEvent()
        )
        // Force the test context to run handled events before testing if the event is processed
        testContext.advanceUntilIdle()
        assertEquals(
            expected = "expected value",
            actual = capturedMetadata.singleOrNull(),
            message = "Expected the verifier client to properly delegate the event of type instantiate contract and use the event processor to extract the additional metadata. Metadata: $capturedMetadata",
        )
    }

    private fun VerifierClientConfig.registerMocks() {
        val mockPbClient = mockk<PbClient>()
        every { acClient.pbClient } returns mockPbClient
        val mockAuthClient = mockk<QueryBlockingStub>()
        every { acClient.pbClient.authClient } returns mockAuthClient
        every { mockAuthClient.account(any()) } returns QueryOuterClass.QueryAccountResponse.newBuilder()
            .setAccount(
                Auth.BaseAccount.newBuilder()
                    .setAddress(verifierAccount.bech32Address)
                    .setAccountNumber(100)
                    .setSequence(0)
                    .build()
                    .toAny()
            )
            .build()
    }
}

object InstantiateContractEventHandler : AssetClassificationEventHandler {
    const val CUSTOM_EVENT_NAME = "instantiate_info"

    override val eventType: ACContractEvent = ACContractEvent.INSTANTIATE_CONTRACT

    override suspend fun handleEvent(parameters: EventHandlerParameters) {
        parameters.eventChannel.send(
            VerifierEvent.CustomEvent(
                eventName = CUSTOM_EVENT_NAME,
                eventBody = InstantiateInfo(data = parameters.event.additionalMetadata!!)
            )
        )
    }
}

data class InstantiateInfo(val data: String)
