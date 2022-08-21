package tech.figure.classification.asset.util.extensions

import io.provenance.client.wallet.NetworkType
import io.provenance.hdwallet.hrp.Hrp

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

fun NetworkType.isProvenanceMainNetAc(): Boolean = this.prefix.lowercase() == Hrp.ProvenanceBlockchain.mainnet
fun NetworkType.isProvenanceTestNetAc(): Boolean = !this.isProvenanceMainNetAc()
