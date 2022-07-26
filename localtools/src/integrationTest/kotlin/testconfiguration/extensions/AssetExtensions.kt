package testconfiguration.extensions

import tech.figure.asset.v1beta1.Asset
import testconfiguration.models.TestAsset

fun Asset.toTestAssetAc(): TestAsset = TestAsset.fromProto(this)
