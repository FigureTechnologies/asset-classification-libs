package testconfiguration.services

import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.metadata.v1.Party
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.RecordInput
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.RecordOutput
import io.provenance.metadata.v1.ResultStatus
import io.provenance.scope.encryption.crypto.Pen
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.objectstore.client.OsClient
import io.provenance.scope.util.MetadataAddress
import io.provenance.scope.util.toByteString
import mu.KLogging
import tech.figure.classification.asset.client.client.base.ACClient
import tech.figure.classification.asset.client.domain.execute.OnboardAssetExecute
import tech.figure.classification.asset.client.domain.model.AccessRoute
import tech.figure.classification.asset.client.domain.model.AssetIdentifier
import tech.figure.classification.asset.util.extensions.wrapListAc
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import tech.figure.spec.AssetSpecifications
import testconfiguration.extensions.getContractSpecFromScopeSpec
import testconfiguration.extensions.toBase64StringAc
import testconfiguration.models.TestAsset
import testconfiguration.util.AppResources
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class AssetOnboardingService(
    private val acClient: ACClient,
    private val osClient: OsClient,
) {
    private companion object : KLogging()

    fun createAsset(
        assetUuid: UUID = UUID.randomUUID(),
        assetType: String = "payable",
        assetMessage: String = "TEST ASSET: $assetUuid",
        ownerAccount: ProvenanceAccountDetail = AppResources.assetOnboardingAccount,
    ): TestAsset = TestAsset(
        assetUuid = assetUuid,
        assetType = assetType,
        message = assetMessage,
        ownerAddress = ownerAccount.bech32Address,
    )

    fun storeAndOnboardTestAsset(
        assetUuid: UUID = UUID.randomUUID(),
        assetType: String = "payable",
        assetMessage: String = "TEST ASSET: $assetUuid",
        ownerAccount: ProvenanceAccountDetail = AppResources.assetOnboardingAccount,
    ): TestAsset {
        val asset = this.createAsset(
            assetUuid = assetUuid,
            assetType = assetType,
            assetMessage = assetMessage,
            ownerAccount = ownerAccount,
        )
        logger.info("Storing TestAsset [$assetUuid] as an Asset in Object Store")
        val assetProto = asset.toProto()
        val assetHash = osClient.put(
            message = assetProto,
            encryptionPublicKey = ownerAccount.publicKey,
            signer = Pen(ProvenanceKeyGenerator.generateKeyPair(ownerAccount.publicKey)),
            additionalAudiences = setOf(AppResources.assetManagerAccount.publicKey),
        ).get(20, TimeUnit.SECONDS).hash.toByteArray().toBase64StringAc()
        logger.info("Successfully added asset [$assetUuid] to Object Store and got hash")
        logger.info("Writing scope for asset [$assetUuid]")
        val assetDefinition = acClient.queryAssetDefinitionByAssetType(assetType)
        val assetSpecification = AssetSpecifications.singleOrNull { it.recordSpecConfigs.single().name == assetDefinition.assetType }
            ?: error("Failed to find asset specification for asset type [$assetType]")
        val scopeMetadata = MetadataAddress.forScope(assetUuid)
        val scopeSpecMetadata = MetadataAddress.forScopeSpecification(assetSpecification.scopeSpecConfig.id)
        val sessionUuid = UUID.randomUUID()
        val sessionMetadata = MetadataAddress.forSession(assetUuid, sessionUuid)
        val contractSpecMetadata = acClient.pbClient.getContractSpecFromScopeSpec(scopeSpecMetadata.toString())
        logger.info("Writing scope with address [$scopeMetadata], scope spec address [$scopeSpecMetadata] and owner [${ownerAccount.bech32Address}]")
        val writeScopeRequest = MsgWriteScopeRequest.newBuilder().also { req ->
            req.scopeUuid = assetUuid.toString()
            req.specUuid = scopeSpecMetadata.getPrimaryUuid().toString()
            req.addSigners(ownerAccount.bech32Address)
            req.scopeBuilder.scopeId = scopeMetadata.bytes.toByteString()
            req.scopeBuilder.specificationId = scopeSpecMetadata.bytes.toByteString()
            req.scopeBuilder.valueOwnerAddress = ownerAccount.bech32Address
            req.scopeBuilder.addOwners(
                Party.newBuilder().also { party ->
                    party.address = ownerAccount.bech32Address
                    party.role = PartyType.PARTY_TYPE_OWNER
                }
            )
            req.scopeBuilder.addAllDataAccess(
                listOf(
                    // Ensure the asset manager is permissioned to access the scope to ensure that object store gateway can process the event
                    AppResources.assetManagerAccount.bech32Address,
                )
            )
        }.build().toAny()
        val writeSessionRequest = MsgWriteSessionRequest.newBuilder().also { req ->
            req.addSigners(ownerAccount.bech32Address)
            req.sessionIdComponentsBuilder.scopeUuid = assetUuid.toString()
            req.sessionIdComponentsBuilder.sessionUuid = sessionUuid.toString()
            req.sessionBuilder.sessionId = sessionMetadata.bytes.toByteString()
            req.sessionBuilder.specificationId = contractSpecMetadata.bytes.toByteString()
            req.sessionBuilder.addParties(
                Party.newBuilder().also { party ->
                    party.address = ownerAccount.bech32Address
                    party.role = PartyType.PARTY_TYPE_OWNER
                }
            )
            req.sessionBuilder.auditBuilder.createdBy = ownerAccount.bech32Address
            req.sessionBuilder.auditBuilder.updatedBy = ownerAccount.bech32Address
        }.build().toAny()
        val recordSpecData = assetSpecification.recordSpecConfigs.singleOrNull()
            ?: error("Expected only a single record spec config for type [${assetDefinition.assetType}] but found: ${assetSpecification.recordSpecConfigs.size}")
        val writeRecordRequest = MsgWriteRecordRequest.newBuilder().also { req ->
            req.contractSpecUuid = contractSpecMetadata.getPrimaryUuid().toString()
            req.addSigners(ownerAccount.bech32Address)
            req.recordBuilder.sessionId = sessionMetadata.bytes.toByteString()
            req.recordBuilder.specificationId = MetadataAddress.forRecordSpecification(contractSpecMetadata.getPrimaryUuid(), assetDefinition.assetType).bytes.toByteString()
            req.recordBuilder.name = assetDefinition.assetType
            req.recordBuilder.addInputs(
                RecordInput.newBuilder().also { input ->
                    input.name = recordSpecData.name
                    input.typeName = recordSpecData.typeClassname
                    input.hash = assetHash
                    input.status = RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED
                }
            )
            req.recordBuilder.addOutputs(
                RecordOutput.newBuilder().also { output ->
                    output.hash = assetHash
                    output.status = ResultStatus.RESULT_STATUS_PASS
                }
            )
            req.recordBuilder.processBuilder.name = "OnboardAssetProcess"
            req.recordBuilder.processBuilder.method = "OnboardAsset"
            // A literal sha256 on the method name: sha256("OnboardTestAsset")
            req.recordBuilder.processBuilder.hash = "32D60974A2B2E9A9D9E93D9956E3A7D2BD226E1511D64D1EA39F86CBED62CE78"
        }.build().toAny()
        val onboardRequest = generateOnboardTestAssetMsg(asset)
        logger.info("Writing scope / session / record to provenance for scope [$scopeMetadata] and onboarding scope to asset classification smart contract")
        acClient.pbClient.estimateAndBroadcastTx(
            txBody = listOf(writeScopeRequest, writeSessionRequest, writeRecordRequest, onboardRequest).toTxBody(),
            signers = BaseReqSigner(signer = ownerAccount.toAccountSigner()).wrapListAc(),
            gasAdjustment = 1.2,
            mode = BroadcastMode.BROADCAST_MODE_BLOCK,
        ).also { response ->
            if (response.txResponse.code != 0) {
                error("Failed to create scope and onboard it: ${response.txResponse.rawLog}")
            }

            val verifier = assetDefinition.verifiers.firstOrNull { it.address == AppResources.verifierAccount.bech32Address } ?: error("AppResources.verifierAccount not found in assetDefinition.verifiers")
            response.txResponse.eventsList
                .firstOrNull { event ->
                    event.type == "assess_custom_msg_fee"
                }?.let { event ->
                    event.attributesList.associate { it.key.toStringUtf8() to it.value.toStringUtf8() }.let { attributes ->
                        assertEquals(AppResources.ONBOARDING_CUSTOM_FEE_NAME, attributes["name"], "Custom onboarding message fee name does not match")
                        assertEquals("${verifier.onboardingCost}${verifier.onboardingDenom}", attributes["amount"], "Custom onboarding message fee amount does not match")
                        assertEquals(acClient.queryContractAddress(), attributes["recipient"], "Custom onboarding message fee does not match contract address")
                    }
                } ?: error("Onboarding transaction did not contain message fee")
        }
        logger.info("Successfully wrote scope / session / record to provenance for scope [$scopeMetadata]")
        return asset
    }

    fun onboardTestAsset(
        asset: TestAsset,
        assetType: String = asset.assetType,
        ownerAccount: ProvenanceAccountDetail,
    ): TestAsset = asset.also {
        assertEquals(
            expected = asset.ownerAddress,
            actual = ownerAccount.bech32Address,
            message = "Provided owner account address is expected to be equal to the TestAsset's owner address",
        )
        acClient.pbClient.estimateAndBroadcastTx(
            txBody = generateOnboardTestAssetMsg(asset = asset, assetType = assetType).toTxBody(),
            signers = BaseReqSigner(signer = ownerAccount.toAccountSigner()).wrapListAc(),
            gasAdjustment = 1.2,
            mode = BroadcastMode.BROADCAST_MODE_BLOCK,
        ).also { response ->
            assertEquals(
                expected = 0,
                actual = response.txResponse.code,
                message = "Expected the transaction response code to be zero, indicating a successful asset onboarding",
            )
        }
    }

    /**
     * Generates an asset classification onboarding message for a given TestAsset.
     *
     * @param asset The asset related to the scope to onboard.
     * @param assetType The type of asset classification to choose.  This value may differ from the TestAsset's initial
     * asset type because assets should be able to be onboarded as multiple types simultaneously.
     */
    private fun generateOnboardTestAssetMsg(
        asset: TestAsset,
        assetType: String = asset.assetType,
    ): com.google.protobuf.Any = acClient.generateOnboardAssetMsg(
        execute = OnboardAssetExecute(
            identifier = AssetIdentifier.AssetUuid(asset.assetUuid),
            assetType = assetType,
            verifierAddress = AppResources.verifierAccount.bech32Address,
            accessRoutes = AccessRoute(
                route = "grpc://localhost:16549",
                name = "gateway",
            ).wrapListAc(),
        ),
        signerAddress = asset.ownerAddress,
    ).toAny()
}
