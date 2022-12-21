package tech.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Base version information about the Asset Classification smart contact.
 *
 * @param contract The internalized name of the smart contract.  Not to be confused with values in the Provenance name
 * meta model.
 * @param version The current version specified in the contract's build file.  This value changes after contract
 * migrations and can help link the running contract to its exposed source code.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class ACVersionInfo(val contract: String, val version: String)
