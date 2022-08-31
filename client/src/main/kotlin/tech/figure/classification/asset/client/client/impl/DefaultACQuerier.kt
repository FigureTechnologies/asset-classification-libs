package tech.figure.classification.asset.client.client.impl

import com.fasterxml.jackson.databind.ObjectMapper
import cosmwasm.wasm.v1.QueryOuterClass
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.queryWasm
import tech.figure.classification.asset.client.client.base.ACQuerier
import tech.figure.classification.asset.client.client.base.ContractIdentifier
import tech.figure.classification.asset.client.domain.NullContractResponseException
import tech.figure.classification.asset.client.domain.model.ACContractState
import tech.figure.classification.asset.client.domain.model.ACVersionInfo
import tech.figure.classification.asset.client.domain.model.AssetDefinition
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.client.domain.model.AssetScopeAttribute
import tech.figure.classification.asset.client.domain.model.QueryAssetDefinitionsResponse
import tech.figure.classification.asset.client.domain.query.QueryAllAssetScopeAttributes
import tech.figure.classification.asset.client.domain.query.QueryAssetDefinition
import tech.figure.classification.asset.client.domain.query.QueryAssetDefinitions
import tech.figure.classification.asset.client.domain.query.QueryAssetScopeAttribute
import tech.figure.classification.asset.client.domain.query.QueryState
import tech.figure.classification.asset.client.domain.query.QueryVersion
import tech.figure.classification.asset.client.domain.query.base.ContractQuery
import java.util.UUID

/**
 * The default override of an [ACQuerier].  Provides all the standard functionality to use an [ACClient][tech.figure.classification.asset.client.client.base.ACClient] if an override for
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
        query = QueryAssetDefinition(assetType),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetDefinitionByAssetType(
        assetType: String,
    ): AssetDefinition = doQuery(
        query = QueryAssetDefinition(
            assetType = assetType,
        ),
    )

    override fun queryAssetDefinitions(): QueryAssetDefinitionsResponse = doQuery(
        query = QueryAssetDefinitions,
    )

    override fun queryAssetScopeAttributeByAssetUuidOrNull(
        assetUuid: UUID,
        assetType: String,
        throwExceptions: Boolean,
    ): AssetScopeAttribute? = doQueryOrNull(
        query = QueryAssetScopeAttribute(
            identifier = AssetIdentifier.AssetUuid(value = assetUuid),
            assetType = assetType
        ),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetScopeAttributeByAssetUuid(
        assetUuid: UUID,
        assetType: String,
    ): AssetScopeAttribute = doQuery(
        query = QueryAssetScopeAttribute(
            identifier = AssetIdentifier.AssetUuid(value = assetUuid),
            assetType = assetType,
        ),
    )

    override fun queryAssetScopeAttributeByScopeAddressOrNull(
        scopeAddress: String,
        assetType: String,
        throwExceptions: Boolean,
    ): AssetScopeAttribute? = doQueryOrNull(
        query = QueryAssetScopeAttribute(
            identifier = AssetIdentifier.ScopeAddress(value = scopeAddress),
            assetType = assetType,
        ),
        throwExceptions = throwExceptions,
    )

    override fun queryAssetScopeAttributeByScopeAddress(
        scopeAddress: String,
        assetType: String,
    ): AssetScopeAttribute = doQuery(
        query = QueryAssetScopeAttribute(
            identifier = AssetIdentifier.ScopeAddress(value = scopeAddress),
            assetType = assetType,
        )
    )

    override fun queryAllAssetScopeAttributesByAssetUuidOrNull(
        assetUuid: UUID,
        throwExceptions: Boolean,
    ): List<AssetScopeAttribute>? = doQueryOrNull(
        query = QueryAllAssetScopeAttributes(
            identifier = AssetIdentifier.AssetUuid(value = assetUuid),
        ),
        throwExceptions = throwExceptions,
    )

    override fun queryAllAssetScopeAttributesByAssetUuid(
        assetUuid: UUID,
    ): List<AssetScopeAttribute> = doQuery(
        query = QueryAllAssetScopeAttributes(
            identifier = AssetIdentifier.AssetUuid(value = assetUuid),
        )
    )

    override fun queryAllAssetScopeAttributesByScopeAddressOrNull(
        scopeAddress: String,
        throwExceptions: Boolean,
    ): List<AssetScopeAttribute>? = doQueryOrNull(
        query = QueryAllAssetScopeAttributes(
            identifier = AssetIdentifier.ScopeAddress(value = scopeAddress),
        ),
        throwExceptions = throwExceptions,
    )

    override fun queryAllAssetScopeAttributesByScopeAddress(
        scopeAddress: String,
    ): List<AssetScopeAttribute> = doQuery(
        query = QueryAllAssetScopeAttributes(
            identifier = AssetIdentifier.ScopeAddress(value = scopeAddress),
        ),
    )

    override fun queryContractState(): ACContractState = doQuery(
        query = QueryState,
    )

    override fun queryContractVersion(): ACVersionInfo = doQuery(
        query = QueryVersion,
    )

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
