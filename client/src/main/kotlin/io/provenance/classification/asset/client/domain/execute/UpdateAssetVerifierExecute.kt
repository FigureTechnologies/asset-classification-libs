package io.provenance.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.execute.base.ContractExecute
import io.provenance.classification.asset.client.domain.model.VerifierDetail

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's update asset
 * verifier execution route.
 *
 * Request usage:
 * ```kotlin
 * val execute = UpdateAssetVerifierExecute(assetType, verifier)
 * val txResponse = acClient.updateAssetVerifier(execute, signer, options)
 * ```
 *
 * @param assetType The type of asset definition that this verifier belongs to.
 * @param verifier The verifier definition that will be update.  This value will be used in an attempt to find an
 * existing verifier on the asset definition that matches the verifier's address field.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("update_asset_verifier")
data class UpdateAssetVerifierExecute(val assetType: String, val verifier: VerifierDetail) : ContractExecute
