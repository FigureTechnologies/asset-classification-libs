package tech.figure.classification.asset.localtools.extensions

import com.google.protobuf.Message
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastTxResponse
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import tech.figure.classification.asset.util.extensions.isErrorAc
import tech.figure.classification.asset.util.extensions.wrapListAc
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail

/**
 * Helper function for the PbClient to take an existing ProvenanceAccountDetail and use it to broadcast a transaction
 * with the given messages.
 */
@JvmName("broadcastTxAcMessage")
fun PbClient.broadcastTxAc(
    messages: List<Message>,
    account: ProvenanceAccountDetail,
    broadcastMode: BroadcastMode = BroadcastMode.BROADCAST_MODE_BLOCK,
    gasAdjustment: Double = 1.2,
    feeGranter: String? = null,
    printRawLogOnFailure: Boolean = true,
): BroadcastTxResponse = broadcastTxAc(
    messages = messages.map { it.toAny() },
    account = account,
    broadcastMode = broadcastMode,
    gasAdjustment = gasAdjustment,
    feeGranter = feeGranter,
    printRawLogOnFailure = printRawLogOnFailure,
)

/**
 * Helper function for the PbClient to take an existing ProvenanceAccountDetail and use it to broadcast a transaction
 * with the given Any-serialized messages.
 */
@JvmName("broadcastTxAcAny")
fun PbClient.broadcastTxAc(
    messages: List<com.google.protobuf.Any>,
    account: ProvenanceAccountDetail,
    broadcastMode: BroadcastMode = BroadcastMode.BROADCAST_MODE_BLOCK,
    gasAdjustment: Double = 1.2,
    feeGranter: String? = null,
    printRawLogOnFailure: Boolean = true,
): BroadcastTxResponse = estimateAndBroadcastTx(
    txBody = messages.toTxBody(),
    signers = account.toAccountSigner().let(::BaseReqSigner).wrapListAc(),
    mode = broadcastMode,
    gasAdjustment = gasAdjustment,
    feeGranter = feeGranter,
).also { response ->
    if (response.isErrorAc()) {
        throw IllegalStateException("Bad response code from transaction${if (printRawLogOnFailure) ". Log: ${response.txResponse.rawLog}" else ""}")
    }
}
