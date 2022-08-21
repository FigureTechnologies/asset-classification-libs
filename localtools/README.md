# Asset Classification Local Tools
This project is intended to be imported as a local library to assist setting up the contract on a local provenance
instance.

## Prerequisites

To setup the asset classification smart contract locally, a few requirements must be met:
- A Provenance Blockchain instance must be running locally.
- The "asset" root name must be bound locally to an account of which the invoker has control.
- The invoker has control of a "contract administrator" account (can be any testnet account) that has a decent amount of hash. About 100 hash will do to be safe indefinitely.
- The invoker has control of a "verifier" account.  This can be any account, even the contract administrator.  For a better experience though, this should be a separate account from the contract admin.

## Using the SetupACTool

To stand up the smart contract on your local Provenance Blockchain instance, simply provide the various configurations
and invoke the tool:

```kotlin
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import java.net.URI
import tech.figure.classification.asset.util.enums.ProvenanceNetworkType
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail

class SetupACToolExample {
    fun exampleUsage() {
        SetupACTool.setupContract(
            config = SetupACToolConfig(
                pbClient = PbClient(
                    chainId = "chain-local",
                    channelUri = URI.create("http://localhost:9090"),
                    gasEstimationMethod = GasEstimationMethod.MSG_FEE_CALCULATION,
                ),
                assetNameAdminAccount = ProvenanceAccountDetail.fromMnemonic(
                    mnemonic = "hub error can bread person quick blur delay nation ignore tennis orphan inch ankle win grunt door turkey ball hockey bridge fragile dose cage",
                    networkType = ProvenanceNetworkType.TESTNET,
                ),
                contractAdminAccount = ProvenanceAccountDetail.fromMnemonic(
                    mnemonic = "grocery want voyage raise betray type vintage offer beach purity mercy manage debate solar blast spray grocery actor remove favorite change bargain mansion tortoise",
                    networkType = ProvenanceNetworkType.TESTNET,
                ),
                verifierBech32Address = "tp1zvkp4yzxky05tt97xce47sucdkzqgqwtza3g6n",
                logger = SetupACToolLogging.Println,
            )
        )
    }
}
```
