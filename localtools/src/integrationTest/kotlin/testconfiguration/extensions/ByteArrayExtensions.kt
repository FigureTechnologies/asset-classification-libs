package testconfiguration.extensions

import java.util.Base64

fun ByteArray.toBase64StringAc(): String = Base64.getEncoder().encodeToString(this)
