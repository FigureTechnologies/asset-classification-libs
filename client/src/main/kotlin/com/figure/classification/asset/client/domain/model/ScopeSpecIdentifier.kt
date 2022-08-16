package com.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.scope.util.MetadataAddress
import java.util.UUID

/**
 * Different keys associated with a scope spec.  Both values are acceptable input to the Asset Classification smart
 * contract.
 */
sealed interface ScopeSpecIdentifier<T> {
    val type: String
    val value: T

    @JsonNaming(SnakeCaseStrategy::class)
    // Order matters because serde in the contract needs a type to anchor the rest of the fields
    @JsonPropertyOrder(value = ["type", "value"])
    class Uuid(override val value: UUID) : ScopeSpecIdentifier<UUID> {
        override val type = "uuid"
    }

    @JsonNaming(SnakeCaseStrategy::class)
    // Order matters because serde in the contract needs a type to anchor the rest of the fields
    @JsonPropertyOrder(value = ["type", "value"])
    class Address(override val value: String) : ScopeSpecIdentifier<String> {
        override val type = "address"
    }

    @JsonIgnore
    fun getScopeSpecUuid(): UUID = when (this) {
        is Uuid -> value
        is Address -> MetadataAddress.fromBech32(value).getPrimaryUuid()
    }

    @JsonIgnore
    fun getScopeSpecAddress(): String = when (this) {
        is Uuid -> MetadataAddress.forScopeSpecification(value).toString()
        is Address -> value
    }
}
