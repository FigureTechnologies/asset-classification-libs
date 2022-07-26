package io.provenance.classification.asset.client.client.impl

import com.fasterxml.jackson.databind.ObjectMapper
import cosmwasm.wasm.v1.QueryOuterClass
import io.provenance.classification.asset.client.client.base.ACQuerier
import io.provenance.classification.asset.client.client.base.ContractIdentifier
import io.provenance.classification.asset.client.domain.NullContractResponseException
import io.provenance.classification.asset.client.domain.model.ACContractState
import io.provenance.classification.asset.client.domain.model.ACVersionInfo
import io.provenance.classification.asset.client.domain.model.AssetDefinition
import io.provenance.classification.asset.client.domain.model.AssetIdentifier
import io.provenance.classification.asset.client.domain.model.AssetQualifier
import io.provenance.classification.asset.client.domain.model.AssetScopeAttribute
import io.provenance.classification.asset.client.domain.model.QueryAssetDefinitionsResponse
import io.provenance.classification.asset.client.domain.query.QueryAssetDefinition
import io.provenance.classification.asset.client.domain.query.QueryAssetDefinitions
import io.provenance.classification.asset.client.domain.query.QueryAssetScopeAttribute
import io.provenance.classification.asset.client.domain.query.QueryState
import io.provenance.classification.asset.client.domain.query.QueryVersion
import io.provenance.classification.asset.client.domain.query.base.ContractQuery
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.queryWasm
import java.util.UUID

/**
 * The default override of an [ACQuerier].  Provides all the standard functionality to use an [ACClient][io.provenance.classification.asset.client.client.base.ACClient] if an override for
 * business logic is not necessary.
 */
class DefaultACQuerier(
    private val contractIdentifier: ContractIdentifier,
    private val objectMapper: ObjectMapper,
    private val pbClient: PbClient,
) : ACQuerier {
    /**
     * This value is cached via a lazy initializer to prevent re-running code against the blockchain after the contract
     * address has been resolved.  The contract address should never change, so this value only needs to be fetched a
     * single time and can be re-used.
     */
    private val cachedContractAddress by lazy { contractIdentifier.resolveAddress(pbClient) }

    override fun queryContractAddress(): String = cachedContractAddress

    override fun queryAssetDefinitionByAssetTypeOrNull(
        assetType: String,
        throwExceptions: Boolean,
    ): AssetDefinition? = doQueryOrNull(
        query = QueryAssetDefinition(AssetQualifier.AssetType(assetType)),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetDefinitionByAssetType(assetType: String): AssetDefinition =
        doQuery(QueryAssetDefinition(AssetQualifier.AssetType(assetType)))

    override fun queryAssetDefinitionByScopeSpecAddressOrNull(
        scopeSpecAddress: String,
        throwExceptions: Boolean,
    ): AssetDefinition? = doQueryOrNull(
        query = QueryAssetDefinition(AssetQualifier.ScopeSpecAddress(scopeSpecAddress)),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetDefinitionByScopeSpecAddress(scopeSpecAddress: String): AssetDefinition =
        doQuery(QueryAssetDefinition(AssetQualifier.ScopeSpecAddress(scopeSpecAddress)))

    override fun queryAssetDefinitions(): QueryAssetDefinitionsResponse = doQuery(QueryAssetDefinitions)

    override fun queryAssetScopeAttributeByAssetUuidOrNull(
        assetUuid: UUID,
        throwExceptions: Boolean,
    ): AssetScopeAttribute? = doQueryOrNull(
        query = QueryAssetScopeAttribute(AssetIdentifier.AssetUuid(assetUuid)),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetScopeAttributeByAssetUuid(assetUuid: UUID): AssetScopeAttribute =
        doQuery(QueryAssetScopeAttribute(AssetIdentifier.AssetUuid(assetUuid)))

    override fun queryAssetScopeAttributeByScopeAddressOrNull(
        scopeAddress: String,
        throwExceptions: Boolean,
    ): AssetScopeAttribute? = doQueryOrNull(
        query = QueryAssetScopeAttribute(AssetIdentifier.ScopeAddress(scopeAddress)),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetScopeAttributeByScopeAddress(scopeAddress: String): AssetScopeAttribute =
        doQuery(QueryAssetScopeAttribute(AssetIdentifier.ScopeAddress(scopeAddress)))

    override fun queryContractState(): ACContractState = doQuery(QueryState)

    override fun queryContractVersion(): ACVersionInfo = doQuery(QueryVersion)

    /**
     * Executes a provided [ContractQuery] against the Asset Classification smart contract.  This relies on the
     * internalized [PbClient] to do the heavy lifting.
     */
    private inline fun <reified T : ContractQuery, reified U : Any> doQuery(query: T): U =
        doQueryOrNull(query)
            ?: throw NullContractResponseException("Received null response from asset classification smart contract for: ${query.queryFailureMessage}")

    private inline fun <reified T : ContractQuery, reified U : Any> doQueryOrNull(query: T): U? =
        pbClient.wasmClient.queryWasm(
            QueryOuterClass.QuerySmartContractStateRequest.newBuilder()
                .setAddress(queryContractAddress())
                .setQueryData(query.toBase64Msg(objectMapper))
                .build()
        ).data
            .toByteArray()
            .let { array -> objectMapper.readValue(array, U::class.java) }
    /**
     * Executes a provided [ContractQuery] against the Asset Classification smart contract with additional functionality
     * designed to return null responses when requested.
     */
    private inline fun <reified T : ContractQuery, reified U : Any> doQueryOrNull(
        query: T,
        throwExceptions: Boolean,
    ): U? = try {
        doQueryOrNull(query)
    } catch (e: Exception) {
        when {
            // Only re-throw caught exceptions if that functionality is requested
            throwExceptions -> throw e
            else -> null
        }
    }
}
