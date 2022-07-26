package io.provenance.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.execute.base.ContractExecute
import io.provenance.classification.asset.client.domain.model.AssetQualifier

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's delete asset
 * definition execution route.  It completely removes the target asset definition, and can cause errors if the
 * definition is currently in use for actively onboarding assets.  In other words: THIS IS DANGEROUS AND SHOULD ONLY BE
 * USED TO REMOVE AN ERRONEOUSLY-ADDED DEFINITION.
 *
 * Sample usage:
 * ```kotlin
 * val deleteByTypeExecute = DeleteAssetDefinitionExecute(AssetQualifier.AssetType("heloc"))
 * val txResponse = acClient.deleteAssetDefinition(deleteByTypeExecute, signer, options)
 *
 * val deleteByScopeSpecExecute = DeleteAssetDefinitionExecute(AssetQualifier.ScopeSpecAddress("scopespec1q3qmhkgypn7prmvg8shhslufxhxqlefpnh"))
 * val txResponse = acClient.deleteAssetDefinition(deleteByScopeSpecExecute, signer, options)
 * ```
 *
 * @param qualifier The asset qualifier used to identify the asset definition to delete.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("delete_asset_definition")
data class DeleteAssetDefinitionExecute(val qualifier: AssetQualifier) : ContractExecute
