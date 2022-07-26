package io.provenance.classification.asset.client.domain.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.provenance.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute

class UpdateAssetDefinitionExecuteSerializer : JsonSerializer<UpdateAssetDefinitionExecute<*>>() {
    override fun serialize(value: UpdateAssetDefinitionExecute<*>, gen: JsonGenerator, provider: SerializerProvider?) {
        // Root node
        gen.writeStartObject()
        // Start update_asset_definition node
        gen.writeObjectFieldStart("update_asset_definition")
        // Start asset_definition node
        gen.writeObjectFieldStart("asset_definition")
        gen.writeStringField("asset_type", value.assetType)
        gen.writeObjectField("scope_spec_identifier", value.scopeSpecIdentifier)
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
