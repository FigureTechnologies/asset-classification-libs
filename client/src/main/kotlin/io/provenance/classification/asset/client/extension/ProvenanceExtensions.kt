package io.provenance.classification.asset.client.extension

import io.provenance.client.wallet.NetworkType
import java.util.Base64

/**
 * Simple helpers to differentiate between main and test net based on network type to avoid similar syntax elsewhere.
 */
internal fun NetworkType.isMainNet(): Boolean = this == NetworkType.MAINNET
internal fun NetworkType.isTestNet(): Boolean = !this.isMainNet()

internal fun ByteArray.base64Encode(): ByteArray = Base64.getEncoder().encode(this)
internal fun ByteArray.base64EncodeString(): String = String(this.base64Encode())