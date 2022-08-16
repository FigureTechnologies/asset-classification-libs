package com.figure.classification.asset.localtools.extensions

import com.figure.classification.asset.util.extensions.toProvenanceTxEventsAc
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastTxResponse

fun BroadcastTxResponse.getCodeIdOrNullAc(): Long? = toProvenanceTxEventsAc()
    .flatMap { it.events }
    .singleOrNull { it.type == "store_code" }
    ?.attributes
    ?.singleOrNull { it.key == "code_id" }
    ?.value
    ?.toLongOrNull()

fun BroadcastTxResponse.getCodeIdAc(): Long = getCodeIdOrNullAc()
    ?: throw IllegalStateException("Unable to retrieve code id from response. Received response log: ${this.txResponse.rawLog}")

fun BroadcastTxResponse.getContractAddressOrNullAc(): String? = toProvenanceTxEventsAc()
    .flatMap { it.events }
    .singleOrNull { it.type == "instantiate" }
    ?.attributes
    ?.singleOrNull { it.key == "_contract_address" }
    ?.value

fun BroadcastTxResponse.getContractAddressAc(): String = getContractAddressOrNullAc()
    ?: throw IllegalStateException("Unable to retrieve contract address from response.  Received response log: ${this.txResponse.rawLog}")
