package com.figure.classification.asset.client.client.base

import com.figure.classification.asset.client.domain.model.ACContractState
import com.figure.classification.asset.client.domain.model.ACVersionInfo
import com.figure.classification.asset.client.domain.model.AssetDefinition
import com.figure.classification.asset.client.domain.model.AssetScopeAttribute
import com.figure.classification.asset.client.domain.model.QueryAssetDefinitionsResponse
import java.util.UUID

/**
 * ACQuerier = Asset Classification Querier
 * This interface defines the different query routes for the Asset Classification smart contract.
 */
interface ACQuerier {
    /**
     * Retrieves the bech32 address assigned to the Asset Classification smart contract based on the current
     * environment.
     */
    fun queryContractAddress(): String

    /**
     * Retrieves an asset definition, if present, from the smart contract by attempting a lookup by asset type.
     * If the definition is not present, null is returned.
     *
     * For a more clear description of its values and purpose, see the [AssetDefinition] class comments.
     *
     * @param assetType The asset type that acts as a unique identifier for the [AssetDefinition].
     * @param throwExceptions If an exception occurs in the smart contract or with the [PbClient][io.provenance.client.grpc.PbClient], it will be re-thrown
     * unless this value is set to false.  An exception is normally thrown by the smart contract when the asset
     * definition is missing, but that exception will be ignored and null will be returned instead, regardless of this
     * boolean's value.
     */
    fun queryAssetDefinitionByAssetTypeOrNull(assetType: String, throwExceptions: Boolean = false): AssetDefinition?

    /**
     * Retrieves an asset definition, if present, from the smart contract by attempting a lookup by asset type.
     * If the definition is not present or any other error occurs, an exception will be thrown.
     *
     * For a more clear description of its values and purpose, see the [AssetDefinition] class comments.
     *
     * @param assetType The asset type that acts as a unique identifier for the [AssetDefinition].
     */
    fun queryAssetDefinitionByAssetType(assetType: String): AssetDefinition

    /**
     * Retrieves an asset definition, if present, from the smart contract by attempting a lookup by scope spec address.
     * If the definition is not present, null is returned.
     *
     * For a more clear description of its values and purpose, see the [AssetDefinition] class comments.
     *
     * @param scopeSpecAddress The bech32 address associated with a scope specification, acting as a unique identifier.
     * Begins with "scopespec".
     * @param throwExceptions If an exception occurs in the smart contract or with the [PbClient][io.provenance.client.grpc.PbClient], it will be re-thrown
     * unless this value is set to false.  An exception is normally thrown by the smart contract when the asset
     * definition is missing, but that exception will be ignored and null will be returned instead, regardless of this
     * boolean's value.
     */
    fun queryAssetDefinitionByScopeSpecAddressOrNull(scopeSpecAddress: String, throwExceptions: Boolean = false): AssetDefinition?

    /**
     * Retrieves an asset definition, if present, from the smart contract by attempting a lookup by scope spec address.
     * If the definition is not present or any other error occurs, an exception will be thrown.
     *
     * For a more clear description of its values and purpose, see the [AssetDefinition] class comments.
     *
     * @param scopeSpecAddress The bech32 address associated with a scope specification, acting as a unique identifier.
     * Begins with "scopespec".
     */
    fun queryAssetDefinitionByScopeSpecAddress(scopeSpecAddress: String): AssetDefinition

    /**
     * Retrieves all asset definitions current stored in the smart contract.
     * This function can be used to dynamically establish a list of all available values without having foreknowledge
     * of each asset type and scope spec address associated with the smart contract.
     */
    fun queryAssetDefinitions(): QueryAssetDefinitionsResponse

    /**
     * Retrieves a scope attribute, if present, on a scope by referencing the scope's asset uuid.
     * If the attribute is not present, null is returned.
     *
     * For a more clear description of its values and purpose, see the [AssetScopeAttribute] class comments.
     *
     * @param assetUuid The asset uuid that correlates to the scope address.
     * @param throwExceptions If an exception occurs in the smart contract or with the [PbClient][io.provenance.client.grpc.PbClient], it will be re-thrown
     * unless this value is set to false.  An exception is normally thrown by the smart contract when the scope
     * attribute is missing, but that exception will be ignored and null will be returned instead, regardless of this
     * boolean's value.
     */
    fun queryAssetScopeAttributeByAssetUuidOrNull(assetUuid: UUID, throwExceptions: Boolean = false): AssetScopeAttribute?

    /**
     * Retrieves a scope attribute, if present, on a scope by referencing the scope's asset uuid.
     * If the attribute is not present or any other error occurs, an exception will be thrown.
     *
     * For a more clear description of its values and purpose, see the [AssetScopeAttribute] class comments.
     *
     * @param assetUuid The asset uuid that correlates to the scope address.
     */
    fun queryAssetScopeAttributeByAssetUuid(assetUuid: UUID): AssetScopeAttribute

    /**
     * Retrieves a scope attribute, if present, on a scope by directly referencing the scope's bech32 address.
     * If the attribute is not present, null is returned.
     *
     * For a more clear description of its values and purpose, see the [AssetScopeAttribute] class comments.
     *
     * @param scopeAddress The bech32 address assigned to the scope.  Begins with "scope"
     * @param throwExceptions If an exception occurs in the smart contract or with the [PbClient][io.provenance.client.grpc.PbClient], it will be re-thrown
     * unless this value is set to false.  An exception is normally thrown by the smart contract when the scope
     * attribute is missing, but that exception will be ignored and null will be returned instead, regardless of this
     * boolean's value.
     */
    fun queryAssetScopeAttributeByScopeAddressOrNull(scopeAddress: String, throwExceptions: Boolean = false): AssetScopeAttribute?

    /**
     * Retrieves a scope attribute, if prsent, on a scope by directly referencing the scope's bech32 address.
     * If the attribute is not present or any other error occurs, an exception will be thrown.
     *
     * For a more clear description of its values and purpose, see the [AssetScopeAttribute] class comments.
     *
     * @param scopeAddress The bech32 address assigned to the scope.  Begins with "scope"
     */
    fun queryAssetScopeAttributeByScopeAddress(scopeAddress: String): AssetScopeAttribute

    /**
     * Retrieves the base contract state for the current environment.  This response notably includes the admin address
     * for the contract.
     */
    fun queryContractState(): ACContractState

    /**
     * Retrieves the contract versioning and name for the current environment.
     */
    fun queryContractVersion(): ACVersionInfo
}
