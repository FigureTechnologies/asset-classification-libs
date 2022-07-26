package testconfiguration

import io.provenance.classification.asset.client.client.base.ACClient
import io.provenance.classification.asset.client.client.base.ContractIdentifier
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import io.provenance.scope.objectstore.client.OsClient
import mu.KLogging
import org.testcontainers.containers.Network
import testconfiguration.containers.ContainerRegistry
import testconfiguration.containers.ManagedContainerType
import testconfiguration.containers.instances.ManagedObjectStoreTestContainer
import testconfiguration.containers.instances.ManagedPostgresTestContainer
import testconfiguration.containers.instances.ManagedProvenanceTestContainer
import testconfiguration.services.InvoiceOnboardingService
import java.net.URI
import java.util.TimeZone
import java.util.UUID
import kotlin.system.exitProcess

abstract class IntTestBase {
    companion object : KLogging() {
        private val network = Network.builder().createNetworkCmdModifier { cmd ->
            cmd.withName("asset-classification-libs-network-${UUID.randomUUID()}")
        }.build()

        private val registry: ContainerRegistry = try {
            // Order matters - if containers depend on each other, make sure they're coded to start up after their
            // dependencies
            ContainerRegistry(network).also { registry ->
                registry.registerAndStart(ManagedPostgresTestContainer())
                registry.registerAndStart(ManagedObjectStoreTestContainer())
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
            registry.getContainer(ManagedContainerType.OBJECT_STORE).let { osContainer ->
                OsClient(
                    uri = URI.create("grpc://localhost:${osContainer.getMappedPort(5000)}"),
                    deadlineMs = 20000L, // 20 seconds til DESTRUCTION
                )
            }
        }

        val invoiceOnboardingService: InvoiceOnboardingService by lazy { InvoiceOnboardingService(acClient, osClient) }
    }

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
