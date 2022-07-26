package testconfiguration

import io.provenance.classification.asset.client.client.base.ACClient
import io.provenance.classification.asset.client.client.base.ContractIdentifier
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import io.provenance.scope.objectstore.client.OsClient
import mu.KLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import testconfiguration.containers.ContainerRegistry
import testconfiguration.containers.ManagedContainerType
import testconfiguration.containers.instances.ManagedProvenanceTestContainer
import java.net.URI
import java.security.Security
import java.util.TimeZone
import kotlin.system.exitProcess

abstract class IntTestBase {
    companion object : KLogging() {
        private val registry: ContainerRegistry = try {
            ContainerRegistry().also { registry ->
                registry.registerAndStart(ManagedProvenanceTestContainer())
            }
        } catch (e: Exception) {
            logger.error("Container startup failed", e)
            exitProcess(1)
        }

        val pbClient: PbClient by lazy {
            registry.getContainer(ManagedContainerType.PROVENANCE).let { provenanceContainer ->
                PbClient(
                    chainId = "chain-local",
                    channelUri = URI.create("http://${provenanceContainer.host}:${provenanceContainer.getMappedPort(9090)}"),
                    gasEstimationMethod = GasEstimationMethod.MSG_FEE_CALCULATION,
                )
            }
        }

        val acClient: ACClient by lazy {
            ACClient.getDefault(
                contractIdentifier = ContractIdentifier.Name("assetclassificationalias.pb"),
                pbClient = pbClient,
            )
        }

        val osClient: OsClient by lazy {
            OsClient(
                // TODO: This is wrong
                uri = URI.create("grpc://localhost:5000"),
                deadlineMs = 20000L, // 20 seconds til DESTRUCTION
            )
        }
    }

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
