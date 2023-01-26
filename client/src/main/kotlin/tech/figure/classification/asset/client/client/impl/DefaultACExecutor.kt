package tech.figure.classification.asset.client.client.impl

import com.fasterxml.jackson.databind.ObjectMapper
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastTxResponse
import cosmwasm.wasm.v1.Tx.MsgExecuteContract
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.grpc.PbClient
import io.provenance.client.grpc.Signer
import io.provenance.client.protobuf.extensions.getBaseAccount
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import tech.figure.classification.asset.client.client.base.ACExecutor
import tech.figure.classification.asset.client.client.base.ACQuerier
import tech.figure.classification.asset.client.client.base.BroadcastOptions
import tech.figure.classification.asset.client.client.extension.buildLogMessage
import tech.figure.classification.asset.client.domain.execute.AddAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.AddAssetVerifierExecute
import tech.figure.classification.asset.client.domain.execute.DeleteAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.OnboardAssetExecute
import tech.figure.classification.asset.client.domain.execute.ToggleAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAccessRoutesExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetVerifierExecute
import tech.figure.classification.asset.client.domain.execute.VerifyAssetExecute
import tech.figure.classification.asset.client.domain.execute.base.ContractExecute
import java.util.Base64

/**
 * The default implementation of an [ACExecutor].  Provides all the standard functionality to use an [ACClient][tech.figure.classification.asset.client.client.base.ACClient] if an
 * override for business logic is not necessary.
 */
class DefaultACExecutor(
    private val objectMapper: ObjectMapper,
    private val pbClient: PbClient,
    private val querier: ACQuerier,
) : ACExecutor {
    override fun <T> generateOnboardAssetMsg(
        execute: OnboardAssetExecute<T>,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun <T> onboardAsset(
        execute: OnboardAssetExecute<T>,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateOnboardAssetMsg(execute, signer.address()), signer, options)

    override fun <T> generateVerifyAssetMsg(
        execute: VerifyAssetExecute<T>,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun <T> verifyAsset(
        execute: VerifyAssetExecute<T>,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateVerifyAssetMsg(execute, signer.address()), signer, options)

    override fun generateAddAssetDefinitionMsg(
        execute: AddAssetDefinitionExecute,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun addAssetDefinition(
        execute: AddAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateAddAssetDefinitionMsg(execute, signer.address()), signer, options)

    override fun generateUpdateAssetDefinitionMsg(
        execute: UpdateAssetDefinitionExecute,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun updateAssetDefinition(
        execute: UpdateAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateUpdateAssetDefinitionMsg(execute, signer.address()), signer, options)

    override fun generateToggleAssetDefinitionMsg(
        execute: ToggleAssetDefinitionExecute,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun toggleAssetDefinition(
        execute: ToggleAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateToggleAssetDefinitionMsg(execute, signer.address()), signer, options)

    override fun generateAddAssetVerifierMsg(
        execute: AddAssetVerifierExecute,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun addAssetVerifier(
        execute: AddAssetVerifierExecute,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateAddAssetVerifierMsg(execute, signer.address()), signer, options)

    override fun generateUpdateAssetVerifierMsg(
        execute: UpdateAssetVerifierExecute,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun updateAssetVerifier(
        execute: UpdateAssetVerifierExecute,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateUpdateAssetVerifierMsg(execute, signer.address()), signer, options)

    override fun <T> generateUpdateAccessRoutesMsg(
        execute: UpdateAccessRoutesExecute<T>,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun <T> updateAccessRoutes(
        execute: UpdateAccessRoutesExecute<T>,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateUpdateAccessRoutesMsg(execute, signer.address()), signer, options)

    override fun generateDeleteAssetDefinitionMsg(
        execute: DeleteAssetDefinitionExecute,
        signerAddress: String,
    ): MsgExecuteContract = generateMsg(execute, signerAddress)

    override fun deleteAssetDefinition(
        execute: DeleteAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse = doExecute(generateDeleteAssetDefinitionMsg(execute, signer.address()), signer, options)

    /**
     * Constructs a generic [MsgExecuteContract] from a provided [ContractExecute] message, ensuring that the provided
     * address is the signer.
     */
    private fun <T : ContractExecute> generateMsg(
        executeMsg: T,
        signerAddress: String,
        funds: Coin? = null,
    ): MsgExecuteContract = MsgExecuteContract.newBuilder().also { executeContract ->
        executeContract.msg = executeMsg.toBase64Msg(objectMapper)
        executeContract.contract = querier.queryContractAddress()
        executeContract.sender = signerAddress
        funds?.also { executeContract.addFunds(it) }
    }.build()

    /**
     * Executes a provided [MsgExecuteContract] with the provided signer information and broadcast mode.  This relies
     * on the internalized [PbClient] to do the heavy lifting.
     */
    private fun doExecute(
        msg: MsgExecuteContract,
        signer: Signer,
        options: BroadcastOptions,
    ): BroadcastTxResponse {
        val signerAddress = signer.address()
        val account = options.baseAccount ?: pbClient.authClient.getBaseAccount(signerAddress)
        return pbClient.estimateAndBroadcastTx(
            txBody = msg.toAny().toTxBody(),
            signers = BaseReqSigner(
                signer = signer,
                sequenceOffset = options.sequenceOffset,
                account = account,
            ).let(::listOf),
            mode = options.broadcastMode,
        ).also { response ->
            if (response.txResponse.code != 0) {
                throw IllegalStateException(
                    buildLogMessage(
                        "Asset classification contract execution failed",
                        "raw log" to response.txResponse.rawLog,
                        "hash" to response.txResponse.txhash,
                        "status code" to response.txResponse.code,
                        "height" to response.txResponse.height,
                        "message" to Base64.getDecoder().decode(msg.msg.toByteArray()),
                        "signing address" to signerAddress,
                    )
                )
            }
        }
    }
}
