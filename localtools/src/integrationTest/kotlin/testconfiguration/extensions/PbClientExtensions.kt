package testconfiguration.extensions

import io.provenance.client.grpc.PbClient
import io.provenance.metadata.v1.ScopeSpecificationRequest
import io.provenance.scope.util.MetadataAddress

fun PbClient.getContractSpecFromScopeSpec(scopeSpecAddress: String): MetadataAddress = metadataClient
    .scopeSpecification(ScopeSpecificationRequest.newBuilder().setSpecificationId(scopeSpecAddress).build())
    .scopeSpecification
    .specification
    .contractSpecIdsList
    .singleOrNull()
    ?.toByteArray()
    ?.let(MetadataAddress::fromBytes)
    ?: throw IllegalStateException("Failed to fetch single contract spec metadata from scope spec address [$scopeSpecAddress]")
