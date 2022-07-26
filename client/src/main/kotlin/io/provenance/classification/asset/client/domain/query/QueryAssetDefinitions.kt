package io.provenance.classification.asset.client.domain.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.query.base.ContractQuery

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's query all asset
 * definitions route.
 * As it has an empty body, it is not required as an input parameter for the default client implementation.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("query_asset_definitions")
object QueryAssetDefinitions : ContractQuery {
    @JsonIgnore
    override val queryFailureMessage: String = "Query all asset definitions"
}
