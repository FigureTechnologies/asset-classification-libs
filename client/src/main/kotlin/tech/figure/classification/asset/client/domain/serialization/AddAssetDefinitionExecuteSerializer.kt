package tech.figure.classification.asset.client.domain.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import tech.figure.classification.asset.client.domain.execute.AddAssetDefinitionExecute

/**
 * The AddAssetDefinitionExecute JSON structure is impossible to represent with vanilla Jackson annotation setups.  This
 * serializer enables the object to be serialized correctly with its multiple-nested nodes without enabling special
 * ObjectMapper features, allowing the object to be more universally applicable to external ObjectMapper singleton
 * instances.
 *
 * This serializer outputs the values in the following format:
 * ```json
 * {
 *      "add_asset_definition": {
 *          "asset_definition": {
 *              ...
 *          }
 *      }
 * }
 * ```
 */
class AddAssetDefinitionExecuteSerializer : JsonSerializer<AddAssetDefinitionExecute<*>>() {
    override fun serialize(value: AddAssetDefinitionExecute<*>, gen: JsonGenerator, provider: SerializerProvider?) {
        // Root node
        gen.writeStartObject()
        // Start add_asset_definition node
        gen.writeObjectFieldStart("add_asset_definition")
        // Start asset_definition node
        gen.writeObjectFieldStart("asset_definition")
        gen.writeStringField("asset_type", value.assetType)
        gen.writeObjectField("scope_spec_identifier", value.scopeSpecIdentifier)
        gen.writeArrayFieldStart("verifiers")
        value.verifiers.forEach { verifier -> gen.writeObject(verifier) }
        gen.writeEndArray()
        value.enabled?.also { enabled -> gen.writeBooleanField("enabled", enabled) }
        value.bindName?.also { bindName -> gen.writeBooleanField("bind_name", bindName) }
        // End asset_definition node
        gen.writeEndObject()
        // End add_asset_definition node
        gen.writeEndObject()
        // End root node
        gen.writeEndObject()
    }
}
