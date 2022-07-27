package io.provenance.classification.asset.client.domain.execute

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.provenance.classification.asset.client.domain.execute.base.ContractExecute

/**
 * This class is a reflection of the request body used in the Asset Classification smart contract's bind contract alias
 * execution route.  It causes the contract to bind the supplied name value to itself, allowing the contract's address
 * to be resolved via the name module.  This should be used with an unrestricted parent name.  A restricted parent name
 * binding will always require that the address that owns the parent address make the binding itself.
 *
 * Sample usage:
 * ```kotlin
 * val bindExecute = BindContractAliasExecute("samplealias.pb")
 * val txResponse = acClient.bindContractAlias(bindExecute, signer, options)
 * ```
 *
 * @param aliasName The fully-qualified name to bind to the contract.  Must be a dot-separated name with a name qualifier
 * and a root name (or a chain from an existing branch from a root name that is unrestricted, ex: test.alias.pb).
 */
@JsonNaming(SnakeCaseStrategy::class)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("bind_contract_alias")
data class BindContractAliasExecute(val aliasName: String) : ContractExecute
