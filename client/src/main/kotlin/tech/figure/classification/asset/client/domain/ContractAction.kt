package tech.figure.classification.asset.client.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.ByteString
import io.provenance.scope.util.toByteString

/**
 * An interface that denotes that instances of the implementor will be able to convert themselves to a [ByteString],
 * which is used in blockchain Tx messages to compose their bodies.
 */
interface ContractAction {
    fun toBase64Msg(objectMapper: ObjectMapper): ByteString = objectMapper.writeValueAsString(this).toByteString()
}
