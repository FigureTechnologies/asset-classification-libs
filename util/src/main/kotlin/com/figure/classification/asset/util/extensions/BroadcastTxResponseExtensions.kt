package com.figure.classification.asset.util.extensions

import com.figure.classification.asset.util.models.ProvenanceTxEvents
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastTxResponse

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

fun BroadcastTxResponse.toProvenanceTxEventsAc(): List<ProvenanceTxEvents> = txResponse.toProvenanceTxEventsAc()

fun BroadcastTxResponse.isErrorAc(): Boolean = this.txResponse.isErrorAc()
fun BroadcastTxResponse.isSuccessAc(): Boolean = !this.isErrorAc()
