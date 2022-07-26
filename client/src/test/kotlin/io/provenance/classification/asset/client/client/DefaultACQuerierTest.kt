package io.provenance.classification.asset.client.client

import cosmwasm.wasm.v1.QueryOuterClass.QuerySmartContractStateResponse
import io.mockk.MockKStubScope
import io.mockk.every
import io.mockk.mockk
import io.provenance.classification.asset.client.client.DefaultACQuerierTest.MockSuite.Companion.DEFAULT_CONTRACT_NAME
import io.provenance.classification.asset.client.client.base.ACQuerier
import io.provenance.classification.asset.client.client.base.ContractIdentifier
import io.provenance.classification.asset.client.client.impl.DefaultACQuerier
import io.provenance.classification.asset.client.domain.NullContractResponseException
import io.provenance.classification.asset.client.domain.model.ACContractState
import io.provenance.classification.asset.client.domain.model.ACVersionInfo
import io.provenance.classification.asset.client.domain.model.AssetDefinition
import io.provenance.classification.asset.client.domain.model.AssetOnboardingStatus
import io.provenance.classification.asset.client.domain.model.AssetScopeAttribute
import io.provenance.classification.asset.client.domain.model.AssetVerificationResult
import io.provenance.classification.asset.client.domain.model.EntityDetail
import io.provenance.classification.asset.client.domain.model.FeeDestination
import io.provenance.classification.asset.client.domain.model.QueryAssetDefinitionsResponse
import io.provenance.classification.asset.client.domain.model.VerifierDetail
import io.provenance.classification.asset.client.helper.OBJECT_MAPPER
import io.provenance.classification.asset.client.helper.assertNotNull
import io.provenance.classification.asset.client.helper.assertNull
import io.provenance.classification.asset.client.helper.assertSucceeds
import io.provenance.classification.asset.client.helper.toJsonPayload
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.queryWasm
import io.provenance.name.v1.QueryResolveResponse
import io.provenance.scope.util.toByteString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class DefaultACQuerierTest {
    @Test
    fun `test queryAssetDefinitionByAssetTypeOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do asset definition things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryAssetDefinitionByAssetTypeOrNull("type", throwExceptions = true)
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetDefinitionByAssetTypeOrNull("type", throwExceptions = false)
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetDefinitionByAssetTypeOrNull("type", throwExceptions = true)
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val assetDefinition = suite.mockAssetDefinition()
        suite.mockQueryReturns(assetDefinition)
        val assetDefinitionFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetDefinitionByAssetTypeOrNull("type")
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = assetDefinition,
            actual = assetDefinitionFromQuery,
            message = "Expected the output to match the asset definition initial value",
        )
    }

    @Test
    fun `test queryAssetDefinitionByAssetType`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do asset definition things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryAssetDefinitionByAssetType("type")
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetDefinitionByAssetType("type")
        }
        val assetDefinition = suite.mockAssetDefinition()
        suite.mockQueryReturns(assetDefinition)
        val assetDefinitionFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetDefinitionByAssetType("type")
        }
        assertEquals(
            expected = assetDefinition,
            actual = assetDefinitionFromQuery,
            message = "Expected the output to match the asset definition initial value",
        )
    }

    @Test
    fun `test queryAssetDefinitionByScopeSpecAddressOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do asset definition things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryAssetDefinitionByScopeSpecAddressOrNull("type", throwExceptions = true)
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetDefinitionByScopeSpecAddressOrNull("type", throwExceptions = false)
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetDefinitionByScopeSpecAddressOrNull("whoaooaoaooa", throwExceptions = true)
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val assetDefinition = suite.mockAssetDefinition()
        suite.mockQueryReturns(assetDefinition)
        val assetDefinitionFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetDefinitionByScopeSpecAddressOrNull("address")
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = assetDefinition,
            actual = assetDefinitionFromQuery,
            message = "Expected the output to match the asset definition initial value",
        )
    }

    @Test
    fun `test queryAssetDefinitionByScopeSpecAddress`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do asset definition things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryAssetDefinitionByScopeSpecAddress("scopeSpecAddress")
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetDefinitionByScopeSpecAddress("type")
        }
        val assetDefinition = suite.mockAssetDefinition()
        suite.mockQueryReturns(assetDefinition)
        val assetDefinitionFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetDefinitionByScopeSpecAddress("myscopespec")
        }
        assertEquals(
            expected = assetDefinition,
            actual = assetDefinitionFromQuery,
            message = "Expected the output to match the asset definition initial value",
        )
    }

    @Test
    fun `test queryAssetDefinitions`() {
        val suite = MockSuite.new()
        val definitions = QueryAssetDefinitionsResponse(
            assetDefinitions = listOf(
                suite.mockAssetDefinition("heloc"),
                suite.mockAssetDefinition("pl"),
                suite.mockAssetDefinition("mortgage"),
            )
        )
        suite.mockQueryReturns(definitions)
        val queryResponse = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetDefinitions()
        }
        assertEquals(
            expected = definitions,
            actual = queryResponse,
            message = "Expected the output to match the definitions input value",
        )
    }

    @Test
    fun `test queryAssetScopeAttributeByAssetUuidOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do scope attribute things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(UUID.randomUUID(), throwExceptions = true)
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(UUID.randomUUID(), throwExceptions = false)
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(UUID.randomUUID(), throwExceptions = true)
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(UUID.randomUUID())
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = attribute,
            actual = attributeFromQuery,
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryAssetScopeAttributeByAssetUuid`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do scope attribute things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryAssetScopeAttributeByAssetUuid(UUID.randomUUID())
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetScopeAttributeByAssetUuid(UUID.randomUUID())
        }
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByAssetUuid(UUID.randomUUID())
        }
        assertEquals(
            expected = attribute,
            actual = attributeFromQuery,
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryAssetScopeAttributeByScopeAddressOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do scope attribute things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull("address", throwExceptions = true)
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull("address", throwExceptions = false)
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull("address", throwExceptions = true)
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull("randomscopeaddress")
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = attribute,
            actual = attributeFromQuery,
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryAssetScopeAttributeByScopeAddress`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do scope attribute things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryAssetScopeAttributeByScopeAddress("address")
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetScopeAttributeByScopeAddress("address")
        }
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByScopeAddress("randomscopeaddress")
        }
        assertEquals(
            expected = attribute,
            actual = attributeFromQuery,
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryContractState`() {
        val suite = MockSuite.new()
        val state = ACContractState(baseContractName = DEFAULT_CONTRACT_NAME, admin = "no-u")
        suite.mockQueryReturns(state)
        val stateFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryContractState()
        }
        assertEquals(
            expected = state,
            actual = stateFromQuery,
            message = "Expected the output to match the input state",
        )
    }

    @Test
    fun `test queryContractVersion`() {
        val suite = MockSuite.new()
        val version = ACVersionInfo(contract = "asset-classification-smart-contract", version = "1.4.2.0")
        suite.mockQueryReturns(version)
        val versionFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryContractVersion()
        }
        assertEquals(
            expected = version,
            actual = versionFromQuery,
            message = "Expected the output to match the input version",
        )
    }

    private data class MockSuite(
        val querier: ACQuerier,
        val pbClient: PbClient,
    ) {
        companion object {
            const val DEFAULT_CONTRACT_NAME = "testassets.pb"
            const val DEFAULT_CONTRACT_ADDRESS = "tp14hj2tavq8fpesdwxxcu44rty3hh90vhujrvcmstl4zr3txmfvw9s96lrg8"

            fun new(contractName: String = DEFAULT_CONTRACT_NAME): MockSuite {
                val pbClient = mockk<PbClient>()
                every { pbClient.nameClient.resolve(any()) } returns QueryResolveResponse.newBuilder().setAddress(DEFAULT_CONTRACT_ADDRESS).build()
                return MockSuite(
                    querier = DefaultACQuerier(
                        contractIdentifier = ContractIdentifier.Name(contractName),
                        objectMapper = OBJECT_MAPPER,
                        pbClient = pbClient,
                    ),
                    pbClient = pbClient,
                )
            }
        }

        fun <T : Any> mockQueryReturns(value: T) {
            mockQuery { this returns QuerySmartContractStateResponse.newBuilder().setData(value.toJsonPayload()).build() }
        }

        fun mockQueryNullResponse() {
            mockQuery { this returns getNullContractResponse() }
        }

        fun <T : Throwable> mockQueryThrows(t: T) {
            mockQuery { this throws t }
        }

        fun mockAssetDefinition(assetType: String = "heloc"): AssetDefinition = AssetDefinition(
            assetType = assetType,
            scopeSpecAddress = "address",
            verifiers = listOf(
                VerifierDetail(
                    address = "address",
                    onboardingCost = "100".toBigInteger(),
                    onboardingDenom = "nhash",
                    feeDestinations = listOf(
                        FeeDestination(
                            address = "fee1",
                            feeAmount = "20".toBigInteger(),
                        ),
                        FeeDestination(
                            address = "fee2",
                            feeAmount = "30".toBigInteger(),
                        )
                    ),
                    entityDetail = EntityDetail(
                        name = "Entity Name",
                        description = "Does the things with the stuff",
                        homeUrl = "www.github.com",
                        sourceUrl = "https://github.com/duffn/dumb-password-rules"
                    ),
                )
            ),
            enabled = true,
        )

        fun mockScopeAttribute(): AssetScopeAttribute = AssetScopeAttribute(
            assetUuid = UUID.randomUUID(),
            scopeAddress = "randomscopeaddress",
            assetType = "heloc",
            requestorAddress = "requestor",
            verifierAddress = "verifier",
            onboardingStatus = AssetOnboardingStatus.APPROVED,
            latestVerifierDetail = null,
            latestVerificationResult = AssetVerificationResult(
                message = "Validation was pretty good on this here scope",
                success = true,
            ),
            accessDefinitions = emptyList(),
        )

        fun getNullContractResponse(): QuerySmartContractStateResponse = QuerySmartContractStateResponse
            .newBuilder()
            .setData("null".toByteString())
            .build()

        private fun mockQuery(
            queryFn: MockKStubScope<QuerySmartContractStateResponse, QuerySmartContractStateResponse>.() -> Unit
        ) {
            every { pbClient.wasmClient.queryWasm(any()) }.queryFn()
        }
    }
}
