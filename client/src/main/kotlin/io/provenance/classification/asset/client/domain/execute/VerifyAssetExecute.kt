package io.provenance.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.execute.base.ContractExecute
import io.provenance.classification.asset.client.domain.model.AccessRoute
import io.provenance.classification.asset.client.domain.model.AssetIdentifier

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's onboard asset
 * execution route.  Companion object functions are recommended for instantiation due to the body structure complexity.
 *
 * Sample usage:
 * ```kotlin
 * val executeForAsset = VerifyAssetExecute(AssetIdentifier.AssetUuid(UUID.randomUUID()), true, "verify success", listOf("route"))
 * val txResponse = acClient.verifyAsset(executeForAsset, signer, options)
 *
 * val executeForScope = VerifyAssetExecute(AssetIdentifier.ScopeAddress("scope1qpkad3gkpn73rmvt7ype8x3tga7sr3ke68"), true, "MAJOR SUCCESS", listOf("some-route"))
 * val txResponse = acClient.verifyAsset(executeForScope, signer, options)
 * ```
 * @param identifier Identifies the asset by uuid or scope address.
 * @param success Whether or not verification succeeded.
 * @param message A custom message indicating the reason for the chosen verification result.
 * @param accessRoutes An optional field that specifies a location at which the verifier has exposed the asset data to
 * authenticated requesters.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("verify_asset")
class VerifyAssetExecute<T>(
    val identifier: AssetIdentifier<T>,
    val success: Boolean,
    val message: String? = null,
    val accessRoutes: List<AccessRoute>? = null,
) : ContractExecute
