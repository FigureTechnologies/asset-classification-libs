package tech.figure.classification.asset.verifier.client

import tech.figure.classification.asset.client.domain.model.AccessRoute

/**
 * A submission from a user of the VerifierClient that indicates how verification was completed.  Includes all details
 * required to submit a completed verification to the smart contract.
 *
 * @param message A message indicating the result of the verification.  Very useful when verification is submitted as success = false for debugging.
 * @param success If the asset is to be marked as verified, and therefore classified.
 * @param accessRoutes Additional access routes for the asset data provided by the verifier.
 */
data class AssetVerification(
    val message: String,
    val success: Boolean,
    val accessRoutes: List<AccessRoute>? = null
)
