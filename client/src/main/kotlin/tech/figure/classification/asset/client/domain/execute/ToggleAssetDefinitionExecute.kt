package tech.figure.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's toggle asset
 * definition execution route.  It simply toggles the definition from enabled to disabled, or vice versa.
 *
 * Sample usage:
 * ```kotlin
 * val toggleOffFromOn = ToggleAssetDefinitionExecute(assetType, false)
 * val txResponse = acClient.toggleAssetDefinition(toggleOffFromOn, signer, options)
 * ```
 *
 * @param assetType The asset type to be toggled.  This should correspond to an existing asset definition within the contract.
 * @param expectedResult The state of [AssetDefinition.enabled][tech.figure.classification.asset.client.domain.model.AssetDefinition.enabled] after execution completes.  Exists as a latch to ensure
 * that multiple requests processed simultaneously don't have unexpected results.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("toggle_asset_definition")
data class ToggleAssetDefinitionExecute(val assetType: String, val expectedResult: Boolean) : ContractExecute
