package com.figure.classification.asset.util.extensions

import java.util.Base64

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

fun ByteArray.base64EncodeAc(): ByteArray = Base64.getEncoder().encode(this)
fun ByteArray.base64EncodeStringAc(): String = String(this.base64EncodeAc())
