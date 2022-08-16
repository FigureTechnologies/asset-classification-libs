package com.figure.classification.asset.verifier.testhelpers

import com.figure.classification.asset.client.domain.model.AccessDefinition
import com.figure.classification.asset.client.domain.model.AccessDefinitionType
import com.figure.classification.asset.client.domain.model.AccessRoute
import com.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import com.figure.classification.asset.client.domain.model.AssetScopeAttribute
import com.figure.classification.asset.client.domain.model.AssetVerificationResult
import com.figure.classification.asset.util.enums.ProvenanceNetworkType
import com.figure.classification.asset.util.extensions.wrapListAc
import com.figure.classification.asset.util.wallet.ProvenanceAccountDetail
import io.provenance.hdwallet.bip39.MnemonicWords
import io.provenance.scope.util.MetadataAddress
import java.util.UUID

fun getMockScopeAttribute(
    assetUuid: UUID = UUID.randomUUID(),
    onboardingStatus: AssetOnboardingStatus = AssetOnboardingStatus.PENDING,
): AssetScopeAttribute = AssetScopeAttribute(
    assetUuid = assetUuid,
    scopeAddress = MetadataAddress.forScope(assetUuid).toString(),
    assetType = "mockasset",
    requestorAddress = "requestor",
    verifierAddress = "verifier",
    onboardingStatus = onboardingStatus,
    latestVerificationResult = onboardingStatus.takeIf { it != AssetOnboardingStatus.PENDING }?.let { status ->
        AssetVerificationResult(
            message = if (status == AssetOnboardingStatus.APPROVED) "MOCK: Approved" else "MOCK: Denied",
            success = status == AssetOnboardingStatus.APPROVED,
        )
    },
    accessDefinitions = AccessDefinition(
        ownerAddress = "requestor",
        accessRoutes = AccessRoute(
            route = "http://mocks.mock",
            name = "gateway",
        ).wrapListAc(),
        definitionType = AccessDefinitionType.REQUESTOR,
    ).wrapListAc(),
)

fun getMockAccountDetail(
    mnemonic: String = MnemonicWords.generate().toString(),
): ProvenanceAccountDetail = ProvenanceAccountDetail.fromMnemonic(
    mnemonic = mnemonic,
    networkType = ProvenanceNetworkType.TESTNET,
)
