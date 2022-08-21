package tech.figure.classification.asset.client.client.base

import io.provenance.client.grpc.PbClient
import io.provenance.name.v1.QueryResolveRequest

/**
 * Different constructors for identifying a contract.  Prefer using [Address] if that value is held,
 * because resolution via name requires blockchain interaction.
 */
sealed class ContractIdentifier {
    /**
     * A reference to a Provenance Blockchain Name Module value that is bound to a specific contract.
     */
    class Name(val contractName: String) : ContractIdentifier()

    /**
     * A direct reference to the contract's bech32 address.
     */
    class Address(val contractAddress: String) : ContractIdentifier()

    fun resolveAddress(pbClient: PbClient): String = when (this) {
        is Name ->
            pbClient
                .nameClient
                .resolve(QueryResolveRequest.newBuilder().setName(contractName).build())
                .address
        is Address -> contractAddress
    }
}
