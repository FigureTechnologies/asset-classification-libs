package tech.figure.classification.asset.util.extensions

import com.google.protobuf.Message
import com.google.protobuf.MessageOrBuilder

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

inline fun <reified T : Message> MessageOrBuilder.buildDynamicAc(): T = try {
    if (this is Message.Builder) {
        this.build() as T
    } else {
        this as T
    }
} catch (e: Exception) {
    throw IllegalArgumentException("Failed to build Message type [${this::class.qualifiedName}]", e)
}

inline fun <reified T : Message.Builder> MessageOrBuilder.toBuilderDynamicAc(): T = try {
    if (this is Message) {
        this.toBuilder() as T
    } else {
        this as T
    }
} catch (e: Exception) {
    throw IllegalArgumentException("Failed to construct builder for type [${this::class.qualifiedName}]", e)
}
