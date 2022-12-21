package tech.figure.classification.asset.util.extensions

import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastTxResponse
import tech.figure.classification.asset.util.models.ProvenanceTxEvents

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

fun BroadcastTxResponse.toProvenanceTxEventsAc(): List<ProvenanceTxEvents> = txResponse.toProvenanceTxEventsAc()

fun BroadcastTxResponse.isErrorAc(): Boolean = this.txResponse.isErrorAc()
fun BroadcastTxResponse.isSuccessAc(): Boolean = !this.isErrorAc()
