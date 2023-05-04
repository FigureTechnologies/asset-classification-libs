package tech.figure.classification.asset.client.client.base

import cosmos.auth.v1beta1.Auth.BaseAccount
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode

/**
 * Various overrides to alter the way in which transactions are broadcast when using the [ACExecutor].
 *
 * @param broadcastMode The mode to use when broadcasting the transaction with [estimateAndBroadcastTx][io.provenance.client.grpc.PbClient.estimateAndBroadcastTx].
 * @param sequenceOffset An offset value to add to the sequence number of the account used. Allows for custom sequence tracking.
 * @param baseAccount The account to use for signing the transaction.  Should match the signer provided in the request.  If none is provided, this value will automatically be looked up with the [AuthClient][io.provenance.client.grpc.PbClient.authClient].
 */
data class BroadcastOptions(
    val broadcastMode: BroadcastMode = BroadcastMode.BROADCAST_MODE_BLOCK,
    val sequenceOffset: Int = 0,
    val baseAccount: BaseAccount? = null
)
