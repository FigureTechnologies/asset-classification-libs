package tech.figure.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute
import tech.figure.classification.asset.client.domain.model.AccessRoute
import tech.figure.classification.asset.client.domain.model.AssetIdentifier

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's update access routes
 * execution route.  Companion object functions are recommended for instantiation due to the body structure complexity.
 *
 * Sample usage:
 * ```kotlin
 * val executeForAsset = UpdateAccessRoutesExecute(AssetIdentifier.AssetUuid(UUID.randomUUID()), "heloc", ownerAddress, routes)
 * val txResponse = acClient.updateAccessRoutes(executeForAsset, signer, options)
 *
 * val executeForScope = UpdateAccessRoutesExecute(AssetIdentifier.ScopeAddress("scope1qq4kmly2pn73rmdydd0k58wt4djq2na9yg"), "heloc", ownerAddress, routes)
 * val txResponse = acClient.updateAccessRoutes(executeForAsset, signer, options)
 * ```
 *
 * @param identifier Identifiers the asset containing the access routes by uuid or scope address.
 * @param assetType The type of asset for which this update will occur.  Assets may include multiple types, so this
 *                  links a specific scope attribute to the update.
 * @param ownerAddress The bech32 address listed on an [AccessDefinition][tech.figure.classification.asset.client.domain.model.AccessDefinition] on the target [AssetScopeAttribute][tech.figure.classification.asset.client.domain.model.AssetScopeAttribute].
 * @param accessRoutes All the new access routes to include in the [AccessDefinition][tech.figure.classification.asset.client.domain.model.AccessDefinition].  Note: All existing routes will be
 *                     completely removed and replaced with these values.  Additionally, this list can be empty, which
 *                     is accepted input and will cause the existing access routes for the target definition to be
 *                     deleted.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("update_access_routes")
data class UpdateAccessRoutesExecute<T>(
    val identifier: AssetIdentifier<T>,
    val assetType: String,
    val ownerAddress: String,
    val accessRoutes: List<AccessRoute>
) : ContractExecute
