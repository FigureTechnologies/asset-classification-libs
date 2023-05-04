package tech.figure.classification.asset.verifier.provenance

/**
 * An enum representation of all events that the asset classification smart contract can emit.
 *
 * @param contractName The value emitted by the smart contract for this event type.
 */
enum class ACContractEvent(val contractName: String) {
    ADD_ASSET_DEFINITION("add_asset_definition"),
    ADD_ASSET_VERIFIER("add_asset_verifier"),
    DELETE_ASSET_DEFINITION("delete_asset_definition"),
    INSTANTIATE_CONTRACT("instantiate_contract"),
    MIGRATE_CONTRACT("migrate_contract"),
    ONBOARD_ASSET("onboard_asset"),
    TOGGLE_ASSET_DEFINITION("toggle_asset_definition"),
    UPDATE_ACCESS_ROUTES("update_access_routes"),
    UPDATE_ASSET_DEFINITION("update_asset_definition"),
    UPDATE_ASSET_VERIFIER("update_asset_verifier"),
    VERIFY_ASSET("verify_asset")
    ;

    companion object {
        private val CONTRACT_NAME_MAP: Map<String, ACContractEvent> by lazy { values().associateBy { it.contractName } }

        fun forContractName(name: String): ACContractEvent = CONTRACT_NAME_MAP[name] ?: throw IllegalArgumentException("Unknown ContractEvent variant [$name]")
    }
}
