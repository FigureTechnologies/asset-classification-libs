# Asset Classification Libs
This project contains libraries for communicating with the [Asset Classification Smart Contract](https://github.com/FigureTechnologies/asset-classification-smart-contract)

## Status
[![Latest Release][release-badge]][release-latest]
[![Maven Central][maven-badge]][maven-url]
[![Apache 2.0 License][license-badge]][license-url]
[![LOC][loc-badge]][loc-report]

[license-badge]: https://img.shields.io/github/license/FigureTechnologies/asset-classification-libs.svg
[license-url]: https://github.com/FigureTechnologies/asset-classification-libs/blob/main/LICENSE
[maven-badge]: https://maven-badges.herokuapp.com/maven-central/tech.figure.classification.asset/ac-client/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/tech.figure.classification.asset/ac-client
[release-badge]: https://img.shields.io/github/tag/FigureTechnologies/asset-classification-libs.svg
[release-latest]: https://github.com/FigureTechnologies/asset-classification-libs/releases/latest
[loc-badge]: https://tokei.rs/b1/github/FigureTechnologies/asset-classification-libs
[loc-report]: https://github.com/FigureTechnologies/asset-classification-libs

## Compatibility

The following client/verifier versions should be used with the asset classification smart contract:

| Client / Verifier | AC Smart Contract |
|-------------------|-------------------|
| v3.1.0+           | v3.1.0+           |
| v3.0.0+           | v3.0.0+           |
| v2.0.0 - v2.0.2   | v2.0.0 - v2.0.1   |
| v1.1.3 - v1.3.0   | v1.0.3 - v1.0.8   |
| v1.1.2 and below  | v1.0.2 and below  |

## Importing the Client and/or Verifier
- The [client](client) library can be downloaded via: `tech.figure.classification.asset:ac-client:<latest-release-version>`
- The [verifier](verifier) library can be downloaded via: `tech.figure.classification.asset:ac-verifier:<latest-release-version>`

*IMPORTANT:* The client and verifier both bundle their dependencies as API dependencies, and will overwrite or be 
overwritten by an implementing project.  This was done purposefully, because the versioning on the various Provenance
dependencies to these projects is fairly complex.  The project is guaranteed to work as shipped only if its provided
dependencies match the version they ship with.  See: [libs.versions.toml](gradle/libs.versions.toml) to inspect the various 
versions listed in their respective dependency bundles.  

Links:
- [Client Dependencies](client/build.gradle.kts)
- [Verifier Dependencies](verifier/build.gradle.kts)

## Using the ACClient
### Creating an ACClient instance
To establish an [ACClient](client/src/main/kotlin/tech/figure/classification/asset/client/client/base/ACClient.kt), first,
create a [PbClient](https://github.com/provenance-io/pb-grpc-client-kotlin/blob/main/src/main/kotlin/io/provenance/client/grpc/PbClient.kt). 
The `PbClient` comes pre-bundled with the client artifact, when imported.  The `PbClient` controls which provenance 
instance the application is communicating with, and, importantly, the provenance instance to which the Asset 
Classification smart contract is deployed.  Then, with the `PbClient` instance, create your `ACClient`.

#### Example:

```kotlin
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import java.net.URI
import tech.figure.classification.asset.client.client.base.ACClient
import tech.figure.classification.asset.client.client.base.ContractIdentifier

class SampleConfiguration {
  fun buildClients() {
    // First, you'll need a PbClient
    val pbClient = PbClient(
      // chain-local for local, other some provenance instance chain id
      chainId = "my-chain-id",
      // http://localhost:9090 for local, or some non-local channel uri
      channelUri = URI("my-channel-uri"),
      // or GasEstimationMethod.COSMOS_SIMULATION
      gasEstimationMethod = GasEstimationMethod.MSG_FEE_CALCULATION
    )
    // Then, the ACClient will know where to look for the Asset Classification smart contract
    // The root interfaces are exposed if you want to create your own implementation, but a default implementation can
    // easily be built simply by using the default function in the companion object of the ACClient interface:
    val acClient = ACClient.getDefault(
      // testassets.pb for local, or some other contract name. 
      // Alternatively, if the contract's bech32 address is directly known, you can use ContractIdentifier.Address("mycontractaddressbech32")
      contractIdentifier = ContractIdentifier.Name("mycontractname"),
      pbClient = pbClient,
      // This is the default and can be omitted, but it exists for if you'd like to provide your own Jackson ObjectMapper instance
      objectMapper = ACObjectMapperUtil.OBJECT_MAPPER,
    )
  }
}
```

## Using the VerifierClient
The [VerifierClient](verifier/src/main/kotlin/tech/figure/classification/asset/verifier/client/VerifierClient.kt) is still
in active development and is not considered ready for use.  This will be updated once that process is completed.  Use at 
your own risk!
