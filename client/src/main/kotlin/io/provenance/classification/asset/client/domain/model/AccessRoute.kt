package io.provenance.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Defines a route by which to access asset data.
 *
 * @param route The route to use (defined by the creator, and understood by the consumer).
 * @param name A free-form name the define the purpose of the route.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class AccessRoute(val route: String, val name: String?)
