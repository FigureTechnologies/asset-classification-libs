package com.figure.classification.asset.util.wallet

import com.figure.classification.asset.util.enums.ProvenanceNetworkType
import com.figure.classification.asset.util.extensions.base64EncodeStringAc
import com.figure.classification.asset.util.objects.ACMetadataKeyUtil
import com.google.common.io.BaseEncoding
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.model.DirectKeyRef
import io.provenance.scope.encryption.model.KeyRef
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.encryption.util.toKeyPair
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Derives and contains all relevant key information for a given account based on various initializers, allowing a one-
 * stop shop for all account information that might be needed for provenance's various crypto utils.
 */
data class ProvenanceAccountDetail(
    val bech32Address: String,
    val publicKey: PublicKey,
    val privateKey: PrivateKey,
    val encodedPublicKey: String,
    val encodedPrivateKey: String,
    val keyRef: KeyRef,
) {
    companion object {
        fun fromJavaPrivateKey(
            privateKey: PrivateKey,
            mainNet: Boolean,
            privateKeyEncoded: String? = null,
        ): ProvenanceAccountDetail =
            privateKey.toKeyPair().let { keyPair ->
                ProvenanceAccountDetail(
                    bech32Address = keyPair.public.getAddress(mainNet),
                    publicKey = keyPair.public,
                    privateKey = keyPair.private,
                    encodedPublicKey = ECUtils.convertPublicKeyToBytes(keyPair.public).base64EncodeStringAc(),
                    encodedPrivateKey = privateKeyEncoded ?: ACMetadataKeyUtil.getBase64EncodedPrivateKey(privateKey),
                    keyRef = DirectKeyRef(keyPair),
                )
            }

        fun fromBase64PrivateKey(privateKeyEncoded: String, mainNet: Boolean): ProvenanceAccountDetail =
            fromJavaPrivateKey(
                privateKey = ECUtils.convertBytesToPrivateKey(BaseEncoding.base64().decode(privateKeyEncoded)),
                mainNet = mainNet,
                privateKeyEncoded = privateKeyEncoded,
            )

        fun fromMnemonic(
            mnemonic: String,
            networkType: ProvenanceNetworkType,
            passphrase: String = "",
        ): ProvenanceAccountDetail =
            fromBase64PrivateKey(
                privateKeyEncoded = ACMetadataKeyUtil.getBase64EncodedPrivateKey(mnemonic, networkType, passphrase),
                mainNet = networkType.isMainNet(),
            )
    }

    fun toAccountSigner(): AccountSigner = AccountSigner.fromAccountDetail(this)
}
