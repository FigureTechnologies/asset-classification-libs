package com.figure.classification.asset.util.objects

import com.figure.classification.asset.util.enums.ProvenanceNetworkType
import com.figure.classification.asset.util.extensions.base64EncodeStringAc
import io.provenance.hdwallet.bip39.MnemonicWords
import io.provenance.hdwallet.ec.extensions.toJavaECPrivateKey
import io.provenance.hdwallet.wallet.Wallet
import io.provenance.scope.encryption.ecies.ECUtils
import java.security.PrivateKey

object ACMetadataKeyUtil {
    fun getBase64EncodedPrivateKey(
        mnemonic: String,
        networkType: ProvenanceNetworkType = ProvenanceNetworkType.TESTNET,
        passphrase: String = "",
    ): String =
        Wallet.fromMnemonic(
            hrp = networkType.prefix,
            passphrase = passphrase,
            mnemonicWords = MnemonicWords.of(mnemonic),
            testnet = networkType.isTestNet(),
        )[networkType.hdPath]
            .keyPair
            .privateKey
            .let(ACMetadataKeyUtil::getBase64EncodedPrivateKey)

    fun getBase64EncodedPrivateKey(
        privateKey: io.provenance.hdwallet.ec.PrivateKey
    ): String = getBase64EncodedPrivateKey(privateKey.toJavaECPrivateKey())

    fun getBase64EncodedPrivateKey(
        privateKey: PrivateKey,
    ): String = ECUtils.convertPrivateKeyToBytes(privateKey).base64EncodeStringAc()
}
