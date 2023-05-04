package testconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import io.provenance.scope.objectstore.client.OsClient
import mu.KLogging
import org.testcontainers.containers.Network
import tech.figure.classification.asset.client.client.base.ACClient
import tech.figure.classification.asset.client.client.base.ContractIdentifier
import tech.figure.classification.asset.client.domain.execute.DeleteAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.model.AssetDefinition
import tech.figure.classification.asset.localtools.tool.SetupACTool
import tech.figure.classification.asset.util.objects.ACObjectMapperUtil
import testconfiguration.containers.ContainerRegistry
import testconfiguration.containers.ManagedContainerType
import testconfiguration.containers.instances.ManagedObjectStoreTestContainer
import testconfiguration.containers.instances.ManagedPostgresTestContainer
import testconfiguration.containers.instances.ManagedProvenanceTestContainer
import testconfiguration.services.AssetOnboardingService
import testconfiguration.util.AppResources
import java.net.URI
import java.util.TimeZone
import java.util.UUID
import kotlin.system.exitProcess
import kotlin.test.BeforeTest

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
                    gasEstimationMethod = GasEstimationMethod.MSG_FEE_CALCULATION
                )
            }
        }

        val acClient: ACClient by lazy {
            ACClient.getDefault(
                contractIdentifier = ContractIdentifier.Name("assetclassificationalias.pb"),
                pbClient = pbClient
            )
        }

        val osClient: OsClient by lazy {
            registry.getContainer(ManagedContainerType.OBJECT_STORE).let { osContainer ->
                OsClient(
                    uri = URI.create("grpc://localhost:${osContainer.getMappedPort(5000)}"),
                    deadlineMs = 20000L // 20 seconds til DESTRUCTION
                )
            }
        }

        val assetOnboardingService: AssetOnboardingService by lazy { AssetOnboardingService(acClient, osClient) }

        val objectMapper: ObjectMapper by lazy { ACObjectMapperUtil.getObjectMapper() }
    }

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /**
     * In order to allow tests to freely modify asset definitions in the contract without affecting other tests, all
     * asset definitions must be removed and the defaults must be re-added before each test executes.
     */
    @BeforeTest
    fun resetAssetDefinitions() {
        logger.info("Resetting asset definitions before test")
        val messages = acClient.queryAssetDefinitions()
            .map { assetDefinition ->
                acClient.generateDeleteAssetDefinitionMsg(
                    execute = DeleteAssetDefinitionExecute(assetDefinition.assetType),
                    signerAddress = AppResources.contractAdminAccount.bech32Address
                )
            }
            .plus(
                SetupACTool
                    .generateAssetDefinitionExecutes(verifierBech32Address = AppResources.verifierAccount.bech32Address)
                    .map {
                        acClient.generateAddAssetDefinitionMsg(
                            execute = it,
                            signerAddress = AppResources.contractAdminAccount.bech32Address
                        )
                    }
            )
            .map { it.toAny() }
        pbClient.estimateAndBroadcastTx(
            txBody = messages.toTxBody(),
            signers = BaseReqSigner(AppResources.contractAdminAccount.toAccountSigner()).let(::listOf),
            mode = BroadcastMode.BROADCAST_MODE_BLOCK
        )
        logger.info("Successfully reset default asset definitions")
    }

    fun updateAssetDefinition(assetDefinition: AssetDefinition) {
        logger.info("Updating asset definition for asset type [${assetDefinition.assetType}]")
        acClient.updateAssetDefinition(
            execute = UpdateAssetDefinitionExecute(
                assetType = assetDefinition.assetType,
                displayName = assetDefinition.displayName,
                verifiers = assetDefinition.verifiers,
                enabled = assetDefinition.enabled
            ),
            signer = AppResources.contractAdminAccount.toAccountSigner()
        )
        logger.info("Successfully updated asset definition for asset type [${assetDefinition.assetType}")
    }
}
