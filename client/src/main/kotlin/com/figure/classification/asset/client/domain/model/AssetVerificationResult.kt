package com.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * A result from a verifier, denoting whether or not the asset is classified as its chosen type.
 *
 * @param message A free-form message from the verifier, explaining its choice in verification true/false.
 * @param success Whether or not the asset is verified.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class AssetVerificationResult(val message: String, val success: Boolean)
