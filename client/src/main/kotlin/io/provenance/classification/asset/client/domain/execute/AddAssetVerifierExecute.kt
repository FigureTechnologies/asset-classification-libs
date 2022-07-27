package io.provenance.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.execute.base.ContractExecute
import io.provenance.classification.asset.client.domain.model.VerifierDetail

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's add asset verifier
 * execution route.
 *
 * Request usage:
 * ```kotlin
 * val execute = AddAssetVerifierExecute(assetType, verifier)
 * val txResponse = acClient.addAssetVerifier(execute, signer, options)
 * ```
 *
 * @param assetType The type of asset definition that this verifier will belong/belongs to.
 * @param verifier The verifier definition that will be newly established.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("add_asset_verifier")
data class AddAssetVerifierExecute(val assetType: String, val verifier: VerifierDetail) : ContractExecute
