package com.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.scope.util.MetadataAddress
import java.util.UUID

/**
 * Defines an asset by asset uuid or scope address.  The contract sees these values are interchangeable.
 */
sealed interface AssetIdentifier<T> {
    val type: String
    val value: T

    @JsonNaming(SnakeCaseStrategy::class)
    // Order matters because serde in the contract needs a type to anchor the rest of the fields
    @JsonPropertyOrder(value = ["type", "value"])
    class AssetUuid(override val value: UUID) : AssetIdentifier<UUID> {
        override val type: String = "asset_uuid"
    }

    @JsonNaming(SnakeCaseStrategy::class)
    // Order matters because serde in the contract needs a type to anchor the rest of the fields
    @JsonPropertyOrder(value = ["type", "value"])
    class ScopeAddress(override val value: String) : AssetIdentifier<String> {
        override val type: String = "scope_address"
    }

    @JsonIgnore
    fun getAssetUuid(): UUID = when (this) {
        is AssetUuid -> value
        is ScopeAddress -> MetadataAddress.fromBech32(value).getPrimaryUuid()
    }

    @JsonIgnore
    fun getScopeAddress(): String = when (this) {
        is AssetUuid -> MetadataAddress.forScope(value).toString()
        is ScopeAddress -> value
    }
}
