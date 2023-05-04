package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * The root subsequent classifications node for a verifier detail.  Contains the default subsequent costs for onboarding
 * an asset with this verifier after already classifying it as a different type with the same verifier.
 *
 * @param cost The onboarding cost to use when classifying an asset using the associated verifier after having already
 * classified it as a different type with the same verifier.  If not set, the default verifier costs are used.
 * @param applicableAssetTypes Specifies the asset types that an asset can be to have the subsequent classification cost
 * apply to them.  If an asset has been classified as any of the types in this list, the cost will be used.  If the list
 * is supplied as a null variant, any subsequent classifications will use the cost.  This value will be rejected if it
 * is supplied as an empty, non-null list.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class SubsequentClassificationDetail(
    val cost: OnboardingCost? = null,
    val applicableAssetTypes: List<String>? = null
)
