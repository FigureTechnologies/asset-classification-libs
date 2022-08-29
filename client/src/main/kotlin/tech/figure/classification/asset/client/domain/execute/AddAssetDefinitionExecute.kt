package tech.figure.classification.asset.client.domain.execute

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute
import tech.figure.classification.asset.client.domain.model.VerifierDetail
import tech.figure.classification.asset.client.domain.serialization.AddAssetDefinitionExecuteSerializer

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's add asset
 * definition execution route.
 *
 * To use it, simply create the execute class and call the appropriate function:
 * ```kotlin
 * val execute = AddAssetDefinitionExecute(assetType, verifiers, enabled = true)
 * val txResponse = acClient.addAssetDefinition(execute, signer, options)
 * ```
 *
 * @param assetType The type of asset that will be added. This value is a unique key in the contract.
 * @param verifiers All verifiers that are allowed to do verification for this specific asset type.
 * @param enabled Whether or not this asset type will accept incoming onboard requests.  If left null, the default value used will be `true`
 * @param bindName Whether or not to bind the name value creating an asset definition.
 */
@JsonSerialize(using = AddAssetDefinitionExecuteSerializer::class)
data class AddAssetDefinitionExecute(
    val assetType: String,
    val verifiers: List<VerifierDetail>,
    val enabled: Boolean? = null,
    val bindName: Boolean? = null,
) : ContractExecute
