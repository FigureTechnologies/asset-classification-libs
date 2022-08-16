package com.figure.classification.asset.client.domain.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * The response from the query asset definitions request.
 *
 * @param assetDefinitions Contains all asset definitions stored in the Asset Classification smart contract.
 */
@JsonNaming(SnakeCaseStrategy::class)
data class QueryAssetDefinitionsResponse(val assetDefinitions: List<AssetDefinition>)
