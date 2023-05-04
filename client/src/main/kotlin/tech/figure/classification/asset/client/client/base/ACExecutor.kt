package tech.figure.classification.asset.client.client.base

import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastTxResponse
import cosmwasm.wasm.v1.Tx.MsgExecuteContract
import io.provenance.client.grpc.Signer
import tech.figure.classification.asset.client.domain.execute.AddAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.AddAssetVerifierExecute
import tech.figure.classification.asset.client.domain.execute.DeleteAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.OnboardAssetExecute
import tech.figure.classification.asset.client.domain.execute.ToggleAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAccessRoutesExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetDefinitionExecute
import tech.figure.classification.asset.client.domain.execute.UpdateAssetVerifierExecute
import tech.figure.classification.asset.client.domain.execute.VerifyAssetExecute

/**
 * ACExecutor = Asset Classification Executor
 * This interface defines the different execution routes for the Asset Classification smart contract, as well as
 * generation functions for the underlying contract execution messages for when the caller desires to collect properly-
 * formatted execution messages for a batched transaction.
 */
interface ACExecutor {
    /**
     * Base [MsgExecuteContract] used during the [onboardAsset] call.  Use this function to get a message that can be
     * bundled with other messages in a transaction.
     *
     * @param execute The [OnboardAssetExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun <T> generateOnboardAssetMsg(
        execute: OnboardAssetExecute<T>,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to classify a given scope as a specific asset type.
     * See: [OnboardAssetExecute] class for descriptions of each portion of the request and its usages.
     *
     * @param execute The [OnboardAssetExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun <T> onboardAsset(
        execute: OnboardAssetExecute<T>,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Builds a [MsgExecuteContract] used during the [verifyAsset] call.  Use this function to get a message that can be
     * bundled with other messages in a transaction.
     *
     * @param execute The [VerifyAssetExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun <T> generateVerifyAssetMsg(
        execute: VerifyAssetExecute<T>,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to verify a given scope as the specified asset type. This
     * functionality is intended to be run by a verifier that has derived the underlying data for an asset and confirmed
     * that it indeed meets the standards of its specified asset type.
     *
     * @param execute The [VerifyAssetExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun <T> verifyAsset(
        execute: VerifyAssetExecute<T>,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Builds a [MsgExecuteContract] used during the [addAssetDefinition] call.  Use this function to get a message that can
     * be bundled with other messages in a transaction.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [AddAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun generateAddAssetDefinitionMsg(
        execute: AddAssetDefinitionExecute,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to add a new asset definition to allow a new asset type to be
     * registered with the smart contract.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [AddAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun addAssetDefinition(
        execute: AddAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Base [MsgExecuteContract] used during the [updateAssetDefinition] call.  Use this function to get a message that can
     * be bundled with other messages in a transaction.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [UpdateAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun generateUpdateAssetDefinitionMsg(
        execute: UpdateAssetDefinitionExecute,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to update an existing asset definition to modify the specified
     * values for an asset type.  This is an all encompassing function to modify all validators at once, and all other
     * aspects of the asset definition.  If you intend to simply modify or add a validator, use those respective routes
     * instead.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [UpdateAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun updateAssetDefinition(
        execute: UpdateAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Base MsgExecuteContract used during the [updateAccessRoutes] call.  Use this function to get a message that can be
     * bundled with other messages in a transaction.
     *
     * This route will only succeed if the admin address or address in [ownerAddress][tech.figure.classification.asset.client.domain.execute.UpdateAccessRoutesExecute.ownerAddress] is used.
     *
     * @param execute The [UpdateAccessRoutesExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun <T> generateUpdateAccessRoutesMsg(
        execute: UpdateAccessRoutesExecute<T>,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to update an existing set of [AccessRoutes][tech.figure.classification.asset.client.domain.model.AccessRoute] for an owner of an
     * [AccessDefinition][tech.figure.classification.asset.client.domain.model.AccessDefinition] on an [AssetScopeAttribute][tech.figure.classification.asset.client.domain.model.AssetScopeAttribute].
     * This function will completely replace all existing [AccessRoutes][tech.figure.classification.asset.client.domain.model.AccessRoute] for the target owner with the provided values.
     *
     * This route will only succeed if the admin address or address in [ownerAddress][tech.figure.classification.asset.client.domain.execute.UpdateAccessRoutesExecute.ownerAddress] is used.
     *
     * @param execute The [UpdateAccessRoutesExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun <T> updateAccessRoutes(
        execute: UpdateAccessRoutesExecute<T>,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Builds a [MsgExecuteContract] used during the [toggleAssetDefinition] call.  Use this function to get a message that
     * can be bundled with other messages in a transaction.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [ToggleAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun generateToggleAssetDefinitionMsg(
        execute: ToggleAssetDefinitionExecute,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to toggle the enabled status for an asset definition. This
     * functionality will completely disable new assets of the specified type from being uploaded, or re-enable a
     * disabled definition.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [ToggleAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun toggleAssetDefinition(
        execute: ToggleAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Builds a [MsgExecuteContract] used during the [addAssetVerifier] call.  Use this function to get a message that can
     * be bundled with other messages in a transaction.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [AddAssetVerifierExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun generateAddAssetVerifierMsg(
        execute: AddAssetVerifierExecute,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to add a new verifier to an asset definition, allowing a new
     * address to verify assets of the specified type.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [AddAssetVerifierExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun addAssetVerifier(
        execute: AddAssetVerifierExecute,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Builds a [MsgExecuteContract] used during the [updateAssetVerifier] call.  Use this function to get a message that
     * can be bundled with other messages in a transaction.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [UpdateAssetVerifierExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun generateUpdateAssetVerifierMsg(
        execute: UpdateAssetVerifierExecute,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to update an existing verifier of an asset definition, generally
     * for changing the fee structure within.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [UpdateAssetVerifierExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun updateAssetVerifier(
        execute: UpdateAssetVerifierExecute,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse

    /**
     * Builds a [MsgExecuteContract] used during the [deleteAssetDefinition] call.  Use this function to get a message that
     * can be bundled with other messages in a transaction.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [DeleteAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signerAddress The address that will sign this message when executed in a transaction.
     */
    fun generateDeleteAssetDefinitionMsg(
        execute: DeleteAssetDefinitionExecute,
        signerAddress: String
    ): MsgExecuteContract

    /**
     * Executes the Asset Classification smart contract to delete an [AssetDefinition][tech.figure.classification.asset.client.domain.model.AssetDefinition]
     * currently stored in the contract's state.
     *
     * ADMIN ONLY! Use: [queryContractState][tech.figure.classification.asset.client.client.base.ACQuerier.queryContractState] to find the admin address.
     *
     * @param execute The [DeleteAssetDefinitionExecute] payload that will be sent to the smart contract.
     * @param signer Any implementation of [Signer] to sign the message programmatically.  See [AccountSigner][tech.figure.classification.asset.util.wallet.AccountSigner] for a provided implementation.
     * @param options Various options that alter how the transaction is broadcast.  See [BroadcastOptions] for more details.
     */
    fun deleteAssetDefinition(
        execute: DeleteAssetDefinitionExecute,
        signer: Signer,
        options: BroadcastOptions = BroadcastOptions()
    ): BroadcastTxResponse
}
