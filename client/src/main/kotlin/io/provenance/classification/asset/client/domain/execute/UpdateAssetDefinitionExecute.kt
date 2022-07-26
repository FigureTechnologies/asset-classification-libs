package io.provenance.classification.asset.client.domain.execute

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.provenance.classification.asset.client.domain.execute.base.ContractExecute
import io.provenance.classification.asset.client.domain.model.ScopeSpecIdentifier
import io.provenance.classification.asset.client.domain.model.VerifierDetail

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's update asset
 * definition execution route.
 *
 * To use it, simply create the execute class and call the appropriate function:
 * ```kotlin
 * val execute = UpdateAssetDefinitionExecute(assetType, ScopeSpecIdentifier.Uuid(UUID.randomUUID()), verifiers, enabled = true)
 * val txResponse = acClient.addAssetDefinition(execute, signer, options)
 * ```
 *
 * @param assetType The type of asset that will be updated. This value is a unique key in the contract.
 * @param scopeSpecIdentifier Identifies the scope spec that this asset definition is associated with. This value is a unique constraint and can only be mapped to one asset definition.
 * @param verifiers All verifiers that are allowed to do verification for this specific asset type.
 * @param enabled Whether or not this asset type will accept incoming onboard requests.  If left null, the default value used will be `true`
 */
@JsonSerialize(using = UpdateAssetDefinitionSerializer::class)
data class UpdateAssetDefinitionExecute<T>(
    val assetType: String,
    val scopeSpecIdentifier: ScopeSpecIdentifier<T>,
    val verifiers: List<VerifierDetail>,
    val enabled: Boolean?,
) : ContractExecute

class UpdateAssetDefinitionSerializer : JsonSerializer<UpdateAssetDefinitionExecute<*>>() {
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
