package io.provenance.classification.asset.client.domain.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.model.AssetQualifier
import io.provenance.classification.asset.client.domain.query.base.ContractQuery

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's query asset
 * definition route.  It is internally utilized in the [ACQuerier][io.provenance.classification.asset.client.client.base.ACQuerier].
 *
 * @param qualifier Qualifies the requested asset by type or scope spec address, which are both unique constraints for
 * each asset definition.
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("query_asset_definition")
data class QueryAssetDefinition(val qualifier: AssetQualifier) : ContractQuery {
    @JsonIgnore
    override val queryFailureMessage: String = "Query asset definition: $qualifier"
}
