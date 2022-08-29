package tech.figure.classification.asset.localtools.tool

import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import cosmwasm.wasm.v1.Tx
import cosmwasm.wasm.v1.Types
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import io.provenance.name.v1.MsgBindNameRequest
import io.provenance.name.v1.NameRecord
import io.provenance.scope.util.toByteString
import io.provenance.spec.AssetSpecifications
import tech.figure.classification.asset.client.client.base.ACClient
import tech.figure.classification.asset.client.client.base.ContractIdentifier
import tech.figure.classification.asset.client.domain.execute.AddAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.model.EntityDetail
import tech.figure.classification.asset.client.domain.model.VerifierDetail
import tech.figure.classification.asset.localtools.extensions.broadcastTxAc
import tech.figure.classification.asset.localtools.extensions.checkNotNullAc
import tech.figure.classification.asset.localtools.extensions.getCodeIdAc
import tech.figure.classification.asset.localtools.extensions.getContractAddressAc
import tech.figure.classification.asset.localtools.extensions.gzipAc
import tech.figure.classification.asset.localtools.feign.GitHubApiClient
import tech.figure.classification.asset.localtools.models.contract.AssetClassificationContractInstantiate
import tech.figure.classification.asset.util.extensions.isErrorAc
import tech.figure.classification.asset.util.extensions.wrapListAc
import tech.figure.classification.asset.util.objects.ACObjectMapperUtil
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import java.io.File
import java.net.URL

object SetupACTool {
    private val OBJECT_MAPPER by lazy { ACObjectMapperUtil.getObjectMapper() }

    fun setupContract(config: SetupACToolConfig) {
        val contractAddress = downloadAndInstantiateSmartContract(config)
        setupAssetDefinitions(config, contractAddress)
    }

    private fun getCompressedWasmBytes(config: SetupACToolConfig): ByteArray =
        when (config.wasmLocation) {
            is ContractWasmLocation.GitHub -> {
                config.logger("Querying GitHub for a download link to the asset classification smart contract's WASM file")
                GitHubApiClient.new().let { client ->
                    // If the release tag is provided in the configuration, attempt to download that tag and throw an exception
                    // if the release is missing
                    config.wasmLocation.contractReleaseTag?.let { tag ->
                        client.getReleaseByTag(
                            organization = "provenance-io",
                            repository = "asset-classification-smart-contract",
                            tag = tag,
                        )
                    } ?: client.getLatestRelease(
                        organization = "provenance-io",
                        repository = "asset-classification-smart-contract",
                    )
                }.assets
                    .singleOrNull { it.name == "asset_classification_smart_contract.wasm" }
                    .checkNotNullAc { "Expected an asset in the asset-classification-smart-contract repository to be the WASM file for the contract" }
                    .browserDownloadUrl
                    .let(::URL)
                    .also { config.logger("Found WASM file at browser download link [$it]. Downloading...") }
                    .readBytes()
                    .also { config.logger("Successfully downloaded wasm file and got uncompressed bytes of size [${it.size}]") }
            }
            is ContractWasmLocation.LocalFile -> {
                when (config.wasmLocation) {
                    is ContractWasmLocation.LocalFile.AbsolutePath -> {
                        File(config.wasmLocation.absoluteFilePath)
                    }
                    is ContractWasmLocation.LocalFile.ProjectResource -> {
                        ClassLoader.getSystemResource(config.wasmLocation.resourcePath).file.let(::File)
                    }
                }
                    .also { config.logger("Loading asset classification smart contract bytes from local file [${it.absolutePath}]") }
                    .readBytes()
            }
        }
            .also { config.logger("GZipping wasm file bytes to compress initial size [${it.size}]") }
            // Incredibly important step: The upstream server requires that all stored wasm bytes are 500K or less, but
            // contracts themselves can be larger than that.  The server supports receipt of GZipped WASM payloads, so this
            // compression allows the Asset Classification Smart Contract (502Kish at the time of writing) to be uploaded
            // without server rejection.
            // Note: Future versions of wasmd (where this 500K check is made) support up to 800K, which should make this
            // compression irrelevant.
            .gzipAc()
            .also { bytes -> config.logger("Successfully compressed wasm bytes. Final byte size: [${bytes.size}]") }

    private fun downloadAndInstantiateSmartContract(config: SetupACToolConfig): String {
        val wasmBytes = getCompressedWasmBytes(config)
        config.logger("Storing code on Provenance Blockchain using address [${config.contractAdminAccount.bech32Address}] as the contract admin")
        val codeId = config.pbClient.broadcastTxAc(
            messages = Tx.MsgStoreCode.newBuilder().also { storeCode ->
                storeCode.instantiatePermission = Types.AccessConfig.newBuilder().also { accessConfig ->
                    accessConfig.address = config.contractAdminAccount.bech32Address
                    accessConfig.permission = Types.AccessType.ACCESS_TYPE_ONLY_ADDRESS
                }.build()
                storeCode.sender = config.contractAdminAccount.bech32Address
                storeCode.wasmByteCode = wasmBytes.toByteString()
            }.build().wrapListAc(),
            account = config.contractAdminAccount,
            gasAdjustment = 1.1,
        ).getCodeIdAc()
        config.logger("Successfully stored WASM and got code id [$codeId]")
        config.logger("Instantiating asset classification smart contract at code id [$codeId]")
        val contractAddress = config.pbClient.broadcastTxAc(
            messages = Tx.MsgInstantiateContract.newBuilder().also { instantiate ->
                instantiate.admin = config.contractAdminAccount.bech32Address
                instantiate.codeId = codeId
                instantiate.label = "asset-classification"
                instantiate.sender = config.contractAdminAccount.bech32Address
                instantiate.msg = AssetClassificationContractInstantiate(
                    baseContractName = "asset",
                    bindBaseName = false,
                    assetDefinitions = emptyList(),
                    isTest = true,
                ).toBase64Msg(OBJECT_MAPPER)
            }.build().wrapListAc(),
            account = config.contractAdminAccount,
            gasAdjustment = 1.1,
        ).getContractAddressAc()
        config.logger("Successfully instantiated the asset classification smart contract with address [$contractAddress]")
        ACClient.getDefault(
            contractIdentifier = ContractIdentifier.Address(contractAddress),
            pbClient = config.pbClient,
        ).also { acClient ->
            config.contractAliasNames.map { alias ->
                config.logger("Generating restricted contract lookup alias [$alias] using contract admin address [${config.contractAdminAccount.bech32Address}]")
                val (childName, parentName) = alias.split('.').let { it.first() to it.drop(1).joinToString(".") }
                MsgBindNameRequest.newBuilder()
                    .setParent(
                        NameRecord.newBuilder()
                            .setName(parentName)
                            .setAddress(config.contractAdminAccount.bech32Address)
                            .setRestricted(false)
                    ).setRecord(
                        NameRecord.newBuilder()
                            .setName(childName)
                            .setAddress(contractAddress)
                            .setRestricted(true)
                    )
                    .build()
            }.let { aliasMessages ->
                config.logger("Binding all alias names in a single transaction using contract admin address [${config.contractAdminAccount.bech32Address}]")
                config.pbClient.broadcastTxAc(messages = aliasMessages, account = config.contractAdminAccount)
            }
        }
        return contractAddress
    }

    private fun setupAssetDefinitions(config: SetupACToolConfig, contractAddress: String) {
        val messages = AssetSpecifications.flatMap { specification ->
            val specType = specification.recordSpecConfigs.single().name
            config.logger("Generating create scope spec messages for type [$specType]")
            val messages = specification.specificationMsgs(config.contractAdminAccount.bech32Address).toMutableList()
            config.logger("Generating add asset definition message to asset classification contract for type [$specType]")
            messages += ACClient.getDefault(
                contractIdentifier = ContractIdentifier.Address(contractAddress),
                pbClient = config.pbClient,
            ).generateAddAssetDefinitionMsg(
                execute = AddAssetDefinitionExecute(
                    assetType = specType,
                    verifiers = VerifierDetail(
                        address = config.verifierBech32Address,
                        onboardingCost = "100000".toBigInteger(),
                        onboardingDenom = "nhash",
                        feeDestinations = emptyList(),
                        entityDetail = EntityDetail(
                            name = "Provenance Blockchain Verifier: $specType",
                            description = "The standard asset classification verifier provided by the Provenance Blockchain Foundation",
                            homeUrl = "https://provenance.io",
                            sourceUrl = "https://github.com/provenance-io/asset-classification-libs",
                        )
                    ).wrapListAc(),
                    enabled = true,
                    bindName = false,
                ),
                signerAddress = config.contractAdminAccount.bech32Address,
            )
            config.logger("Generating bind name message of type [$specType.asset] to contract address [$contractAddress] for future attribute writes")
            messages += MsgBindNameRequest.newBuilder().also { bindName ->
                bindName.parent = NameRecord.newBuilder().also { nameRecord ->
                    nameRecord.name = "asset"
                    nameRecord.address = config.assetNameAdminAccount.bech32Address
                }.build()
                bindName.record = NameRecord.newBuilder().also { nameRecord ->
                    nameRecord.name = specType
                    nameRecord.address = contractAddress
                    nameRecord.restricted = true
                }.build()
            }.build()
            messages
        }
        config.logger("Broadcasting all generated messages...")
        config.pbClient.estimateAndBroadcastTx(
            txBody = messages.map { it.toAny() }.toTxBody(),
            signers = listOf(
                BaseReqSigner(config.contractAdminAccount.toAccountSigner()),
                BaseReqSigner(config.assetNameAdminAccount.toAccountSigner()),
            ),
            mode = BroadcastMode.BROADCAST_MODE_BLOCK,
            gasAdjustment = 1.2,
        ).also { response ->
            if (response.isErrorAc()) {
                throw IllegalStateException("FAILED to fully create all scope specifications, add asset definitions, and bind asset names to the smart contract. Got raw log: ${response.txResponse.rawLog}")
            }
        }
    }
}

/**
 * Defines how to run the local setup tool, with various toggles for functionality.
 *
 * @param pbClient A client for communication with a local instance of Provenance.
 * @param assetNameAdminAccount The ProvenanceAccountDetail corresponding to the localnet account that controls the
 * "asset" root name.  This account should be manually generated and provided in the root name declarations of your
 * local genesis file.
 * @param contractAdminAccount The ProvenanceAccountDetail corresponding to an account that will be used as the contract
 * administrator.  This admin will be registered with the smart contract and its mnemonic should be locally held and
 * used for all administrative operations (like adding asset definitions to the smart contract).
 * @param verifierBech32Address The bech32 address for the account that will be set as the verifier for all automatically
 * added asset definitions in the smart contract.
 * @param contractAliasNames These names will be registered with the smart contract as lookup aliases, allowing any
 * address to look up the contract's address by name resolution.  These names must branch from an unrestricted address
 * or the setup process will throw an exception.
 * @param wasmLocation An optional parameter that will allow the consumer to decide where the smart contract instance comes
 * from.  By default, the latest version of the smart contract will be downloaded from GitHub.
 * @param logger Defines how the process does logging.  The default is Disabled, which will not log anything unless an
 * exception occurs.
 */
data class SetupACToolConfig(
    val pbClient: PbClient,
    val assetNameAdminAccount: ProvenanceAccountDetail,
    val contractAdminAccount: ProvenanceAccountDetail,
    val verifierBech32Address: String,
    val contractAliasNames: List<String> = listOf("assetclassificationalias.pb", "testassets.pb"),
    val wasmLocation: ContractWasmLocation = ContractWasmLocation.GitHub(),
    val logger: SetupACToolLogging = SetupACToolLogging.Disabled,
)

/**
 * Defines how logging should be handled for the underlying setup process.
 */
sealed interface SetupACToolLogging {
    val log: (message: String) -> Unit

    // Operator function override to allow instances to be used functionality, like SetupACToolLogging.Println("message")
    operator fun invoke(message: String) {
        log(message)
    }

    // Prints out all logs via std println function
    object Println : SetupACToolLogging {
        override val log: (message: String) -> Unit = { println(it) }
    }

    // Disregards all logs. Only exceptions will be displayed, if they occur.
    object Disabled : SetupACToolLogging {
        override val log: (message: String) -> Unit = {}
    }

    // Allows a custom logging process to be specified.  For instance, if using KotlinLogging, it might look something
    // like this:
    // SetupACToolLogging.Custom { message -> logger.info(message) }
    class Custom(override val log: (message: String) -> Unit) : SetupACToolLogging
}

/**
 * Defines how to locate the Asset Classification smart contract WASM file.
 */
sealed interface ContractWasmLocation {
    /**
     * Denotes that the contract should be downloaded from GitHub.
     *
     * @param contractReleaseTag An optional parameter to specify a custom tag value.  If missing, the latest version
     * will be downloaded.  Example of a valid provided value: v1.0.0
     */
    class GitHub(val contractReleaseTag: String? = null) : ContractWasmLocation

    /**
     * Denotes that the contract should be pulled from a location on the current machine.
     */
    sealed interface LocalFile : ContractWasmLocation {
        /**
         * Denotes that the contract will be pulled using an absolute file path to somewhere on the current system.
         *
         * @param absoluteFilePath The absolute file location.  Must use a path directly from the root directory.  This
         * path should contain the file name.  Example: /Users/yourname/projects/my-app/src/main/resources/smart_contract.wasm
         */
        class AbsolutePath(val absoluteFilePath: String) : LocalFile

        /**
         * Denotes that the contract will be pulled from the current project's resource folder.  This assumes that the
         * file is in the standard resource directory that are included in project compilation.
         * Example: subfolder/smart_contract.wasm  OR smart_contract.wasm
         *
         * @param resourcePath
         */
        class ProjectResource(val resourcePath: String) : LocalFile
    }
}
