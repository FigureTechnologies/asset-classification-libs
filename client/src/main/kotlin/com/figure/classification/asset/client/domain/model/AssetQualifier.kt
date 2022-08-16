package com.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Different keys associated with an asset type.  Both values are unique keys for an asset type.
 */
sealed interface AssetQualifier {
    val type: String
    val value: String

    @JsonNaming(SnakeCaseStrategy::class)
    // Order matters because serde in the contract needs a type to anchor the rest of the fields
    @JsonPropertyOrder(value = ["type", "value"])
    class AssetType(override val value: String) : AssetQualifier {
        override val type: String = "asset_type"
    }

    @JsonNaming(SnakeCaseStrategy::class)
    // Order matters because serde in the contract needs a type to anchor the rest of the fields
    @JsonPropertyOrder(value = ["type", "value"])
    class ScopeSpecAddress(override val value: String) : AssetQualifier {
        override val type: String = "scope_spec_address"
    }
}
