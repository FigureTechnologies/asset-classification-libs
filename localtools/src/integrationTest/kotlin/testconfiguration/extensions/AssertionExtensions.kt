package testconfiguration.extensions

import kotlin.test.assertNotNull

fun <T : Any> T?.assertNotNullAc(message: String = "Expected value to be non-null"): T {
    assertNotNull(actual = this, message = message)
    return this
}
