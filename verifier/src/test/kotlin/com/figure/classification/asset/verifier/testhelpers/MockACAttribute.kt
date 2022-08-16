package com.figure.classification.asset.verifier.testhelpers

import com.figure.classification.asset.verifier.provenance.ACContractEvent
import com.figure.classification.asset.verifier.provenance.ACContractKey
import io.provenance.eventstream.stream.models.Event

sealed interface MockACAttribute {
    val key: String
    val value: String

    fun toAttribute(): Event = Event(key = key, value = value)

    class EventType(eventType: ACContractEvent) : MockACAttribute {
        override val key: String = ACContractKey.EVENT_TYPE.eventName
        override val value: String = eventType.contractName
    }

    class AssetType(assetType: String) : MockACAttribute {
        override val key: String = ACContractKey.ASSET_TYPE.eventName
        override val value: String = assetType
    }

    class ScopeAddress(scopeAddress: String) : MockACAttribute {
        override val key: String = ACContractKey.SCOPE_ADDRESS.eventName
        override val value: String = scopeAddress
    }

    class VerifierAddress(verifierAddress: String) : MockACAttribute {
        override val key: String = ACContractKey.VERIFIER_ADDRESS.eventName
        override val value: String = verifierAddress
    }

    class ScopeOwnerAddress(scopeOwnerAddress: String) : MockACAttribute {
        override val key: String = ACContractKey.SCOPE_OWNER_ADDRESS.eventName
        override val value: String = scopeOwnerAddress
    }

    class NewValue(newValue: String) : MockACAttribute {
        override val key: String = ACContractKey.NEW_VALUE.eventName
        override val value: String = newValue
    }

    class AdditionalMetadata(additionalMetadata: String) : MockACAttribute {
        override val key: String = ACContractKey.ADDITIONAL_METADATA.eventName
        override val value: String = additionalMetadata
    }
}
