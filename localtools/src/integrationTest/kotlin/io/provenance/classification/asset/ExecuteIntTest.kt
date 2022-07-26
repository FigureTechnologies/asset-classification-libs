package io.provenance.classification.asset

import org.junit.jupiter.api.Test
import testconfiguration.IntTestBase

class ExecuteIntTest : IntTestBase() {
    @Test
    fun testSimpleOnboardAsset() {
        val asset = invoiceOnboardingService.onboardTestAsset()
    }
}
