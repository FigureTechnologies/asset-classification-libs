package tech.figure.classification.asset.localtools.models.contract

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute
import tech.figure.classification.asset.client.domain.model.AssetDefinition

@JsonNaming(SnakeCaseStrategy::class)
data class AssetClassificationContractInstantiate(
    val baseContractName: String,
    val bindBaseName: Boolean,
    val assetDefinitions: List<AssetDefinition>,
    val isTest: Boolean? = null
) : ContractExecute
