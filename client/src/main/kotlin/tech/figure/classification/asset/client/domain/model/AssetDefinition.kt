package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * The root structure for defining how an asset should be onboarded by the Asset Classification smart contract.
 *
 * @param assetType A unique name that defines the type of scopes that pertain to this definition.
 * @param verifiers All different asset verifiers' information for this specific asset type.
 * @param enabled Whether or not this asset type is allowed for onboarding.  Default in the contract is `true`.
 * @param displayName A human-readable version of the name of the asset type. Ex: assetType: heloc, displayName: Home Equity Line of Credit
 */
@JsonNaming(SnakeCaseStrategy::class)
data class AssetDefinition(
    val assetType: String,
    val verifiers: List<VerifierDetail>,
    val enabled: Boolean,
    val displayName: String?,
)
