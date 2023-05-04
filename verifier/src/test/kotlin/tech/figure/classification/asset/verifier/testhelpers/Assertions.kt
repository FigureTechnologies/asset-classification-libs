package tech.figure.classification.asset.verifier.testhelpers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import tech.figure.classification.asset.verifier.config.VerifierEvent
import tech.figure.classification.asset.verifier.event.EventHandlerParameters
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@ExperimentalCoroutinesApi
suspend inline fun <reified T : VerifierEvent> assertNextEvent(
    parameters: EventHandlerParameters,
    assertions: (T) -> Unit = {}
) {
    assertFalse(
        actual = parameters.eventChannel.isEmpty,
        message = "An event should be present in the channel"
    )
    when (val event = parameters.eventChannel.receive()) {
        is T -> assertions(event)
        else -> fail("Expected an event of type [${T::class.simpleName}], but got [${event::class.simpleName}]")
    }
}

@ExperimentalCoroutinesApi
suspend inline fun <reified T : VerifierEvent> assertLastEvent(
    parameters: EventHandlerParameters,
    assertions: (T) -> Unit = {}
) {
    assertNextEvent(parameters, assertions)
    assertTrue(
        actual = parameters.eventChannel.isEmpty,
        message = "Expected no more events to have been emitted"
    )
}
