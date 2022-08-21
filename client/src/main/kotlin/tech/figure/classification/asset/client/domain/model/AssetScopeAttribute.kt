package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.UUID

/**
 * A representation of the json body that is appended to a scope during the Asset Classification smart contract's
 * onboarding and verification processes.
 *
 * @param assetUuid The unique id associated with the specific asset within the scope.
 * @param scopeAddress The bech32 address of the scope that this attribute is appended to.
 * @param assetType The type of asset that this scope is classified as.  Can only be considered valid if the onboarding status is APPROVED.
 * @param requestorAddress The bech32 address of the entity that initiated the onboarding of this asset into the Asset Classification smart contract.
 * @param verifierAddress The bech32 address of the verifier that was chosen by the requestor for verification.
 * @param onboardingStatus Indicates the portion of the process that this asset is in as far as the contract is concerned.  See enum descriptions for more information.
 * @param latestVerificationResult Stores information about the latest verification done for the asset.  This value will only be non-null after its chosen verifier has determined its result.
 * @param accessDefinitions Stores information about how to access this asset at various sources.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class AssetScopeAttribute(
    val assetUuid: UUID,
    val scopeAddress: String,
    val assetType: String,
    val requestorAddress: String,
    val verifierAddress: String,
    val onboardingStatus: AssetOnboardingStatus,
    val latestVerificationResult: AssetVerificationResult?,
    val accessDefinitions: List<AccessDefinition>,
)

/**
 * Denotes the status of the asset during the onboarding and verification process.
 */
enum class AssetOnboardingStatus {
    // Denotes that the asset has been onboarded, but not yet verified
    @JsonProperty("pending")
    PENDING,
    // Denotes that the asset has been rejected by its chosen verifier and must be run through onboarding again to be classified
    @JsonProperty("denied")
    DENIED,
    // Denotes that the asset is fully verified and should be classified as the chosen type
    @JsonProperty("approved")
    APPROVED,
}
