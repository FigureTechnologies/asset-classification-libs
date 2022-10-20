package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * The root subsequent classifications node for a verifier detail.  Contains the default subsequent costs for onboarding
 * an asset with this verifier after already classifying it as a different type with the same verifier.
 *
 * @param cost The onboarding cost to use when classifying an asset using the associated verifier after having already
 * classified it as a different type with the same verifier.  If not set, the default verifier costs are used.
 * @param allowedAssetTypes Specifies the asset types that an asset can already be classified as when using this
 * verifier.  If not set, any asset type may be used.  This value will be rejected if it is supplied as an empty vector.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class SubsequentClassificationDetail(
    val cost: OnboardingCost? = null,
    val allowedAssetTypes: List<String>? = null,
)
