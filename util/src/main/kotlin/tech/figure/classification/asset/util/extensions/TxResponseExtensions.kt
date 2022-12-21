package tech.figure.classification.asset.util.extensions

import com.fasterxml.jackson.core.type.TypeReference
import cosmos.base.abci.v1beta1.Abci.TxResponse
import tech.figure.classification.asset.util.internal.OBJECT_MAPPER
import tech.figure.classification.asset.util.models.ProvenanceTxEvents

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

fun TxResponse.isErrorAc(): Boolean = this.code != 0
fun TxResponse.isSuccessAc(): Boolean = !this.isErrorAc()

/**
 * Expands the rawLow property of the TxResponse into a navigable data structure for logical parsing.
 * When the log corresponds to an error, an exception will be thrown.
 */
fun TxResponse.toProvenanceTxEventsAc(): List<ProvenanceTxEvents> = if (this.code != 0) {
    throw IllegalStateException("Error logs cannot be parsed as ProvenanceTxEvents.  Error message: ${this.rawLog}")
} else {
    OBJECT_MAPPER.readValue(this.rawLog, object : TypeReference<List<ProvenanceTxEvents>>() {})
}
