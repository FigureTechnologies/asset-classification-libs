package tech.figure.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute
import tech.figure.classification.asset.client.domain.model.AccessRoute
import tech.figure.classification.asset.client.domain.model.AssetIdentifier

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's onboard asset
 * execution route.  Companion object functions are recommended for instantiation due to the body structure complexity.
 *
 * Sample usage:
 * ```kotlin
 * val executeForAsset = OnboardAssetExecute(AssetIdentifier.AssetUuid(UUID.randomUUID()), assetType, verifierAddress, routes)
 * val txResponse = acClient.onboardAsset(executeForAsset, signer, options)
 *
 * val executeForScope = OnboardAssetExecute(AssetIdentifier.ScopeAddress("scope1qzuq9fjkpn7prmv08geml38h999qnwke37"), assetType, verifierAddress, routes)
 * val txResponse = acClient.onboardAsset(executeForScope, signer, options)
 * ```
 *
 * @param identifier Identifies the asset by uuid or scope address.
 * @param assetType The type of asset that the scope contains.  Each type should be mapped to a specific scope specification,
 * which can be derived via queries to the contract.
 * @param verifierAddress The address of the verifier to use after onboarding. The available verifiers for each asset
 * type can be found by querying the contract.
 * @param accessRoutes Each verifier should be configured to locate the asset record data via these provided access
 * routes.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("onboard_asset")
data class OnboardAssetExecute<T>(
    val identifier: AssetIdentifier<T>,
    val assetType: String,
    val verifierAddress: String,
    val accessRoutes: List<AccessRoute>? = null,
) : ContractExecute
