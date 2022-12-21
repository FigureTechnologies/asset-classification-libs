package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * The root structure for defining how an asset should be onboarded by the Asset Classification smart contract.
 *
 * @param assetType A unique name that defines the type of scopes that pertain to this definition.
 * @param displayName A pretty human-readable name for this asset type (vs a typically snake_case asset_type name).
 * @param verifiers All different asset verifiers' information for this specific asset type.
 * @param enabled Whether or not this asset type is allowed for onboarding.  Default in the contract is `true`.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class AssetDefinition(
    val assetType: String,
    val displayName: String?,
    val verifiers: List<VerifierDetail>,
    val enabled: Boolean,
)
