package tech.figure.classification.asset.client.domain.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.client.domain.query.base.ContractQuery

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's query asset scope
 * attribute route.  It is internally utilized in the ACQuerier.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("query_asset_scope_attribute")
data class QueryAssetScopeAttribute<T>(val identifier: AssetIdentifier<T>) : ContractQuery {
    @JsonIgnore
    override val queryFailureMessage: String = "Query asset scope attribute by $identifier"
}
