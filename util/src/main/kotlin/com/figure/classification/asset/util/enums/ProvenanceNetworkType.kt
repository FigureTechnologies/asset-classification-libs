package com.figure.classification.asset.util.enums

import io.provenance.client.wallet.NetworkType
import io.provenance.hdwallet.hrp.Hrp

/**
 * An enum detailing various network specifications for the Provenance Blockchain.
 * The wallet library's NetworkType class now has no preset values for Provenance, so these enums serve to be an easy
 * to use collection of different address derivation methods that are used for the Provenance Blockchain.
 */
enum class ProvenanceNetworkType(val prefix: String, val hdPath: String) {
    MAINNET(prefix = Hrp.ProvenanceBlockchain.mainnet, hdPath = "m/44'/505'/0'/0/0"),
    TESTNET(prefix = Hrp.ProvenanceBlockchain.testnet, hdPath = "m/44'/1'/0'/0/0'"),
    COSMOS_TESTNET(prefix = Hrp.ProvenanceBlockchain.testnet, hdPath = "m/44'/1'/0'/0/0");

    fun toNetworkType(): NetworkType = NetworkType(prefix = prefix, path = hdPath)

    fun isMainNet(): Boolean = this == MAINNET

    fun isTestNet(): Boolean = !this.isMainNet()
}
