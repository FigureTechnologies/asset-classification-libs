package tech.figure.classification.asset.client.domain.execute

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute
import tech.figure.classification.asset.client.domain.model.ScopeSpecIdentifier
import tech.figure.classification.asset.client.domain.model.VerifierDetail
import tech.figure.classification.asset.client.domain.serialization.UpdateAssetDefinitionExecuteSerializer

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
@JsonSerialize(using = UpdateAssetDefinitionExecuteSerializer::class)
data class UpdateAssetDefinitionExecute<T>(
    val assetType: String,
    val scopeSpecIdentifier: ScopeSpecIdentifier<T>,
    val verifiers: List<VerifierDetail>,
    val enabled: Boolean? = null,
) : ContractExecute
