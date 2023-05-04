package testconfiguration.util

import tech.figure.classification.asset.util.enums.ProvenanceNetworkType
import tech.figure.classification.asset.util.wallet.ProvenanceAccountDetail

object AppResources {
    // IMPORTANT: This version should be pinned to the latest asset classification smart contract instance that the client
    // and libs are compatible with.  Local testing for unpublished versions of the smart contract's WASM file can be
    // done by tweaking ManagedProvenanceTestContainer's afterStartup function implementation to fetch the file from local
    // resources, if needed.
    const val CONTRACT_VERSION = "3.3.0"
    const val ONBOARDING_CUSTOM_FEE_NAME = "Asset Classification Onboarding Fee"

    // This account is intended to simulate an account used for verifying assets
    val verifierAccount: ProvenanceAccountDetail by lazy {
        ProvenanceAccountDetail.fromMnemonic(
            mnemonic = "noble transfer wedding artwork blue upon phone source effort hungry target casino orange hole roof regular seminar round upgrade stem basic random public engage",
            networkType = ProvenanceNetworkType.TESTNET
        )
    }

    // This account is intended to simulate an account used for the Asset Manager application
    val assetManagerAccount: ProvenanceAccountDetail by lazy {
        ProvenanceAccountDetail.fromMnemonic(
            mnemonic = "trophy clock reject miracle hurdle prefer march display orchard supreme mad flag basket furnace gain stone machine orphan bench undo alter sun donor when",
            networkType = ProvenanceNetworkType.TESTNET
        )
    }

    // This account is intended to simulate an external consumer that onboards an asset for verification
    val assetOnboardingAccount: ProvenanceAccountDetail by lazy {
        ProvenanceAccountDetail.fromMnemonic(
            mnemonic = "alone buyer giant vacuum awesome bread pony require stumble head trumpet energy tribe lunch wish brain era return couple cereal skull help ritual zero",
            networkType = ProvenanceNetworkType.TESTNET
        )
    }

    // This account is seeded in the genesis file and has a large amount of nhash.  It also has a restricted binding for the asset root name and unrestricted for the alias root name
    val assetAdminAccount: ProvenanceAccountDetail by lazy {
        ProvenanceAccountDetail.fromMnemonic(
            mnemonic = "stable payment cliff fault abuse clinic bus belt film then forward world goose bring picnic rich special brush basic lamp window coral worry change",
            networkType = ProvenanceNetworkType.COSMOS_TESTNET
        )
    }

    // This account is intended to be used as the administrator for the Asset Classification smart contract
    val contractAdminAccount: ProvenanceAccountDetail by lazy {
        ProvenanceAccountDetail.fromMnemonic(
            mnemonic = "language kitchen front mistake like mansion require item option pencil install grass symbol reflect height arrest tank young record motor father emotion onion pledge",
            networkType = ProvenanceNetworkType.TESTNET
        )
    }

    val allAccounts: List<ProvenanceAccountQualifier> by lazy {
        listOf(
            verifierAccount named "Verifier",
            assetManagerAccount named "Asset Manager",
            assetOnboardingAccount named "Asset Onboarding Account",
            assetAdminAccount named "Asset Name Owner",
            contractAdminAccount named "Smart Contract Admin"
        )
    }
}

data class ProvenanceAccountQualifier(val name: String, val account: ProvenanceAccountDetail)

private infix fun ProvenanceAccountDetail.named(name: String): ProvenanceAccountQualifier = ProvenanceAccountQualifier(
    name = name,
    account = this
)
