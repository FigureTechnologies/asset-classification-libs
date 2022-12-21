package tech.figure.classification.asset.util.wallet

import com.google.protobuf.ByteString
import cosmos.crypto.secp256k1.Keys
import io.provenance.client.grpc.Signer
import io.provenance.hdwallet.ec.PrivateKey
import io.provenance.hdwallet.ec.PublicKey
import io.provenance.hdwallet.ec.extensions.toECPrivateKey
import io.provenance.hdwallet.ec.extensions.toJavaECPrivateKey
import io.provenance.hdwallet.signer.BCECSigner
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.encryption.util.toKeyPair
import io.provenance.scope.util.sha256
import tech.figure.classification.asset.util.enums.ProvenanceNetworkType

/**
 * A Provenance Signer implementation.  This helper provides multiple ways to derive the signer and eventually boils
 * down to using the public and private keys to sign requests.
 */
class AccountSigner(
    private val address: String,
    private val publicKey: PublicKey,
    private val privateKey: PrivateKey,
) : Signer {
    override fun address(): String = address

    override fun pubKey(): Keys.PubKey =
        Keys.PubKey.newBuilder().setKey(ByteString.copyFrom(publicKey.compressed())).build()

    override fun sign(data: ByteArray): ByteArray = BCECSigner().sign(privateKey, data.sha256()).encodeAsBTC().toByteArray()

    companion object {
        fun fromAccountDetail(
            accountDetail: ProvenanceAccountDetail,
        ): AccountSigner = accountDetail.privateKey.toECPrivateKey().toECKeyPair().let { keyPair ->
            AccountSigner(
                address = accountDetail.bech32Address,
                publicKey = keyPair.publicKey,
                privateKey = keyPair.privateKey,
            )
        }

        fun fromJavaPrivateKey(
            privateKey: java.security.PrivateKey,
            mainNet: Boolean,
        ): AccountSigner = fromECPrivateKey(privateKey.toECPrivateKey(), mainNet)

        fun fromECPrivateKey(
            privateKey: PrivateKey,
            mainNet: Boolean,
        ): AccountSigner = privateKey.toJavaECPrivateKey().toKeyPair().let { keyPair ->
            AccountSigner(
                address = keyPair.public.getAddress(mainNet),
                publicKey = privateKey.toPublicKey(),
                privateKey = privateKey,
            )
        }

        fun fromMnemonic(
            mnemonic: String,
            networkType: ProvenanceNetworkType,
        ): AccountSigner = fromAccountDetail(
            accountDetail = ProvenanceAccountDetail.fromMnemonic(mnemonic, networkType)
        )
    }
}
