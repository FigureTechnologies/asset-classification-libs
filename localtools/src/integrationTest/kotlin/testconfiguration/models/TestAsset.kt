package testconfiguration.models

import com.fasterxml.jackson.module.kotlin.readValue
import com.figure.classification.asset.util.objects.ACObjectMapperUtil
import io.provenance.scope.util.toByteString
import tech.figure.asset.v1beta1.Asset
import java.util.UUID

data class TestAsset(
    val assetUuid: UUID,
    val assetType: String,
    val message: String,
    val ownerAddress: String,
) {
    companion object {
        private val OBJECT_MAPPER = ACObjectMapperUtil.getObjectMapper()

        fun fromProto(asset: Asset): TestAsset = asset.kvMap["testAsset"]
            ?.let { anyAsset ->
                OBJECT_MAPPER.readValue(anyAsset.value.toString(Charsets.UTF_8))
            }
            ?: error("Provided asset with id [${asset.id.value}], type [${asset.type}] and description [${asset.description}] could not be read as a TestAsset")
    }

    fun toProto(): Asset = Asset.newBuilder().also { assetBuilder ->
        assetBuilder.id = tech.figure.util.v1beta1.UUID.newBuilder().setValue(assetUuid.toString()).build()
        assetBuilder.type = assetType
        assetBuilder.description = message
        assetBuilder.putKv(
            "testAsset",
            com.google.protobuf.Any.newBuilder()
                .setTypeUrl("/testconfiguration.models.TestAsset")
                .setValue(OBJECT_MAPPER.writeValueAsString(this).toByteString())
                .build()
        )
    }.build()
}
