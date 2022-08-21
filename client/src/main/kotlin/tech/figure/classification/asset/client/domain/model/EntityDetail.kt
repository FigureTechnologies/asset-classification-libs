package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * An optional set of field that defines an entity associated with the Asset Classification smart contract.
 *
 * @param name A short name describing the entity.
 * @param description A short description of the entity's purpose.
 * @param homeUrl A web link that can send observers to the organization that the verifier belongs to.
 * @param sourceUrl A web link that can send observers to the source code of the verifier, for increased transparency.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class EntityDetail(
    val name: String?,
    val description: String?,
    val homeUrl: String?,
    val sourceUrl: String?,
)
