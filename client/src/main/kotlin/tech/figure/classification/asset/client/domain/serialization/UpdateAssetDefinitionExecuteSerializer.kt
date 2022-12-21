package tech.figure.classification.asset.client.domain.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import tech.figure.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute

/**
 * The UpdateAssetDefinitionExecute JSON structure is impossible to represent with vanilla Jackson annotation setups.  This
 * serializer enables the object to be serialized correctly with its multiple-nested nodes without enabling special
 * ObjectMapper features, allowing the object to be more universally applicable to external ObjectMapper singleton
 * instances.
 *
 * This serializer outputs the values in the following format:
 * ```json
 * {
 *      "update_asset_definition": {
 *          "asset_definition": {
 *              ...
 *          }
 *      }
 * }
 * ```
 */
class UpdateAssetDefinitionExecuteSerializer : JsonSerializer<UpdateAssetDefinitionExecute>() {
    override fun serialize(value: UpdateAssetDefinitionExecute, gen: JsonGenerator, provider: SerializerProvider?) {
        // Root node
        gen.writeStartObject()
        // Start update_asset_definition node
        gen.writeObjectFieldStart("update_asset_definition")
        // Start asset_definition node
        gen.writeObjectFieldStart("asset_definition")
        gen.writeStringField("asset_type", value.assetType)
        value.displayName?.also { displayName -> gen.writeStringField("display_name", displayName) }
        gen.writeArrayFieldStart("verifiers")
        value.verifiers.forEach { verifier -> gen.writeObject(verifier) }
        gen.writeEndArray()
        value.enabled?.also { enabled -> gen.writeBooleanField("enabled", enabled) }
        // End asset_definition node
        gen.writeEndObject()
        // End update_asset_definition node
        gen.writeEndObject()
        // End root node
        gen.writeEndObject()
    }
}
