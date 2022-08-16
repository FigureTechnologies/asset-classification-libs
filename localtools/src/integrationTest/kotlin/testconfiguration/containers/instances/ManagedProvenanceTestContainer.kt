package testconfiguration.containers.instances

import com.figure.classification.asset.localtools.extensions.broadcastTxAc
import com.figure.classification.asset.localtools.tool.ContractWasmLocation
import com.figure.classification.asset.localtools.tool.SetupACTool
import com.figure.classification.asset.localtools.tool.SetupACToolConfig
import com.figure.classification.asset.localtools.tool.SetupACToolLogging
import cosmos.bank.v1beta1.Tx
import cosmos.base.v1beta1.CoinOuterClass
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.getBaseAccount
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import testconfiguration.containers.ManagedContainerType
import testconfiguration.containers.ManagedTestContainer
import testconfiguration.util.AppResources
import testconfiguration.util.CoroutineUtil
import java.net.URI
import java.security.Security

class ManagedProvenanceTestContainer : ManagedTestContainer<ProvenanceTestContainer> {
    private companion object : KLogging() {
        private const val DEFAULT_NHASH_FUNDING_AMOUNT: Long = 20_000_000_000_000
        const val NHASH_DENOM: String = "nhash"
    }

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override val containerType: ManagedContainerType = ManagedContainerType.PROVENANCE

    override fun buildContainer(network: Network): ProvenanceTestContainer = ProvenanceTestContainer()
        .withNetwork(network)
        .withNetworkMode(network.id)
        .withNetworkAliases("provenance")
        .withClasspathResourceMapping("data/provenance", "/home/provenance_seed", BindMode.READ_ONLY)
        .withExposedPorts(1317, 9090, 26657)
        .withCommand("bash", "-c", "cp -rn /home/provenance_seed/* /home/provenance && /usr/bin/provenanced -t --home /home/provenance start")
        .waitingFor(ProvenanceWaitStrategy(AppResources.assetAdminAccount.bech32Address))

    override fun afterStartup(container: ProvenanceTestContainer) {
        logger.info("Establishing smart contract instance...")
        getPbClient(host = container.host, mappedPort = container.getMappedPort(9090)).use { pbClient ->
            fundAssetClassificationAccounts(pbClient)
            SetupACTool.setupContract(
                config = SetupACToolConfig(
                    pbClient = pbClient,
                    assetNameAdminAccount = AppResources.assetAdminAccount,
                    contractAdminAccount = AppResources.contractAdminAccount,
                    verifierBech32Address = AppResources.verifierAccount.bech32Address,
                    contractAliasNames = listOf("assetclassificationalias.pb", "testassets.pb"),
                    wasmLocation = ContractWasmLocation.GitHub(contractReleaseTag = "v${AppResources.CONTRACT_VERSION}"),
                    logger = SetupACToolLogging.Custom(log = logger::info),
                )
            )
        }
    }

    private fun fundAssetClassificationAccounts(pbClient: PbClient) {
        val messages = AppResources
            .allAccounts
            // Fund all accounts exposed by AppResources except the admin account, as it is already funded by the genesis file
            .filterNot { it.account == AppResources.assetAdminAccount }
            .map { (accountName, account) ->
                logger.info("Generating funding message to add [$DEFAULT_NHASH_FUNDING_AMOUNT$NHASH_DENOM] to account [$accountName | ${account.bech32Address}] using admin account [${AppResources.assetAdminAccount.bech32Address}]")
                Tx.MsgSend.newBuilder().also { send ->
                    send.fromAddress = AppResources.assetAdminAccount.bech32Address
                    send.toAddress = account.bech32Address
                    send.addAmount(
                        CoinOuterClass.Coin.newBuilder().also { coin ->
                            coin.amount = DEFAULT_NHASH_FUNDING_AMOUNT.toString()
                            coin.denom = NHASH_DENOM
                        }
                    )
                }.build()
            }
        pbClient.broadcastTxAc(
            messages = messages,
            account = AppResources.assetAdminAccount,
            gasAdjustment = 1.3,
        )
        logger.info("Successfully funded all test accounts")
    }
}

class ProvenanceTestContainer : GenericContainer<ProvenanceTestContainer>("provenanceio/provenance:v1.11.1") {
    private companion object : KLogging()

    init {
        logger.info("Starting Provenance Blockchain container version v1.11.1")
    }
}

class ProvenanceWaitStrategy(private val expectedGenesisAccountBech32: String) : AbstractWaitStrategy() {
    private companion object : KLogging()

    override fun waitUntilReady() {
        try {
            val host = waitStrategyTarget.host
            val port = waitStrategyTarget.getMappedPort(9090)
            logger.info("Starting PbClient at $host:$port[mapped from port 9090]")
            val pbClient = getPbClient(host = host, mappedPort = port)
            logger.info("Checking for genesis account [$expectedGenesisAccountBech32] existence...")
            runBlocking {
                launch {
                    val account = CoroutineUtil.withRetryBackoff(
                        errorPrefix = "Waiting for genesis account [$expectedGenesisAccountBech32] to be created",
                        initialDelay = 1000L,
                        maxDelay = 20000L,
                        showStackTraceInFailures = false,
                        block = { pbClient.authClient.getBaseAccount(expectedGenesisAccountBech32) },
                    )
                    logger.info("Successfully fetched genesis account [${account.address}] with account number [${account.accountNumber}]")
                }.join()
            }
            logger.info("Closing the PbClient instance...")
            pbClient.close()
        } catch (e: Exception) {
            throw ContainerLaunchException("Provenance was not in a healthy state", e)
        }
    }
}

private fun getPbClient(host: String, mappedPort: Int): PbClient = PbClient(
    chainId = "chain-local",
    channelUri = URI.create("http://$host:$mappedPort"),
    gasEstimationMethod = GasEstimationMethod.MSG_FEE_CALCULATION,
)
