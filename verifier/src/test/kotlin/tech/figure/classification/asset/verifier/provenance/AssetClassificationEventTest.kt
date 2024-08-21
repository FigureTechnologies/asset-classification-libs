package tech.figure.classification.asset.verifier.provenance

import cosmos.tx.v1beta1.ServiceOuterClass
import io.provenance.scope.util.toByteString
import tech.figure.classification.asset.verifier.provenance.AssetClassificationEvent.Companion.fromVerifierTxEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class AssetClassificationEventTest {
    @Test
    fun `fromVerifierTxEvents filters and maps wasm events to AssetClassificationEvent`() {
        val tx = ServiceOuterClass.GetTxResponse.newBuilder().apply {
            txResponseBuilder.apply {
                addEventsBuilder().apply {
                    type = WASM_EVENT_TYPE
                    addAttributesBuilder().apply {
                        key = "X2NvbnRyYWN0X2FkZHJlc3M=".toByteString()
                        value = "cGIxbWZ1aDQ5bjIyOW5xbWczeW1tZmg3NGRhcngwNGdqeXZtdzh1OWMK".toByteString()
                        index = false
                    }
                    // "asset_event_type" : "onboard_asset"
                    addAttributesBuilder().apply {
                        key = "YXNzZXRfZXZlbnRfdHlwZQ==".toByteString()
                        value = "b25ib2FyZF9hc3NldA==".toByteString()
                        index = false
                    }
                }

                addEventsBuilder().apply {
                    type = "execute"
                    addAttributesBuilder().apply {
                        key = "X2NvbnRyYWN0X2FkZHJlc3M=".toByteString()
                        value = "cGIxbWZ1aDQ5bjIyOW5xbWczeW1tZmg3NGRhcngwNGdqeXZtdzh1OWMK".toByteString()
                    }
                }
            }
        }.build()

        val events = fromVerifierTxEvents(
            sourceTx = tx,
            txEvents = tx.txResponse.eventsList
        )

        assertEquals(events.size, 1)
        events[0].run {
            assertEquals(eventType, ACContractEvent.ONBOARD_ASSET)
        }
    }
}
