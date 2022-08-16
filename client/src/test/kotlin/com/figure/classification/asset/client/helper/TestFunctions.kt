package com.figure.classification.asset.client.helper

import com.google.protobuf.ByteString
import io.provenance.scope.util.toByteString
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

fun <T> assertSucceeds(message: String = "Expected block to execute without exception", block: () -> T): T = try {
    block()
} catch (e: Exception) {
    fail(message, e)
}

fun <T> T?.assertNotNull(message: String? = null): T {
    assertNotNull(this, message)
    return this
}

fun <T> T?.assertNull(message: String? = null) {
    assertNull(this, message)
}

fun <T> T.toJsonPayload(): ByteString = OBJECT_MAPPER.writeValueAsString(this).toByteString()
