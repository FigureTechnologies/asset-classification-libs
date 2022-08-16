package com.figure.classification.asset.verifier.config

import com.figure.classification.asset.client.domain.model.AccessRoute
import com.figure.classification.asset.client.domain.model.AssetScopeAttribute
import com.figure.classification.asset.verifier.client.AssetVerification
import com.figure.classification.asset.verifier.provenance.AssetClassificationEvent

/**
 * This interface is the driver for the verifier client.  It defines how each specific action should be executed,
 * and exposes numerous events to the client implementor.
 */
interface VerificationProcessor<T> {
    /**
     * After verifying that an asset scope attribute has been correctly written to a scope, this function is called with
     * the scope onboarding requestor's data access routes.  The verifier implementation should use this function as
     * an entrypoint to retrieve the specified asset data from an external service, given these access routes.  The
     * requestor is not required to provide access routes, so the list of routes provided herein is not guaranteed to
     * be populated.
     */
    suspend fun retrieveAsset(
        event: AssetClassificationEvent,
        scopeAttribute: AssetScopeAttribute,
        accessRoutes: List<AccessRoute>,
    ): T?

    /**
     * After the asset is retrieved, this function should verify that it is in the proper shape.  Regardless of the
     * process used herein, the resulting value should be an AssetVerification data class.  This will signify to the
     * Asset Classification Smart Contract whether or not the asset should receive an AssetOnboardingStatus of APPROVED
     * or DENIED, based on the verification's `verifySuccess` parameter.
     */
    suspend fun verifyAsset(
        event: AssetClassificationEvent,
        scopeAttribute: AssetScopeAttribute,
        asset: T,
    ): AssetVerification
}
