package tech.figure.classification.asset.client.client

import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmwasm.wasm.v1.QueryOuterClass.QuerySmartContractStateResponse
import io.mockk.MockKStubScope
import io.mockk.every
import io.mockk.mockk
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.queryWasm
import io.provenance.name.v1.QueryResolveResponse
import io.provenance.scope.util.toByteString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.figure.classification.asset.client.client.DefaultACQuerierTest.MockSuite.Companion.DEFAULT_CONTRACT_NAME
import tech.figure.classification.asset.client.client.base.ACQuerier
import tech.figure.classification.asset.client.client.base.ContractIdentifier
import tech.figure.classification.asset.client.client.impl.DefaultACQuerier
import tech.figure.classification.asset.client.domain.NullContractResponseException
import tech.figure.classification.asset.client.domain.model.ACContractState
import tech.figure.classification.asset.client.domain.model.ACVersionInfo
import tech.figure.classification.asset.client.domain.model.AssetDefinition
import tech.figure.classification.asset.client.domain.model.AssetOnboardingStatus
import tech.figure.classification.asset.client.domain.model.AssetScopeAttribute
import tech.figure.classification.asset.client.domain.model.AssetVerificationResult
import tech.figure.classification.asset.client.domain.model.EntityDetail
import tech.figure.classification.asset.client.domain.model.FeeDestination
import tech.figure.classification.asset.client.domain.model.FeePayment
import tech.figure.classification.asset.client.domain.model.FeePaymentDetail
import tech.figure.classification.asset.client.domain.model.VerifierDetail
import tech.figure.classification.asset.client.helper.OBJECT_MAPPER
import tech.figure.classification.asset.client.helper.assertNotNull
import tech.figure.classification.asset.client.helper.assertNull
import tech.figure.classification.asset.client.helper.assertSucceeds
import tech.figure.classification.asset.client.helper.toJsonPayload
import java.util.Locale
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
    fun `test queryAssetDefinitions`() {
        val suite = MockSuite.new()
        val definitions = listOf(
            suite.mockAssetDefinition("heloc"),
            suite.mockAssetDefinition("pl"),
            suite.mockAssetDefinition("mortgage"),
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
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
                throwExceptions = true,
            )
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
                throwExceptions = false,
            )
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
                throwExceptions = true,
            )
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
            )
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
            suite.querier.queryAssetScopeAttributeByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
            )
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetScopeAttributeByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
            )
        }
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "type",
            )
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
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "type",
                throwExceptions = true,
            )
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "type",
                throwExceptions = false,
            )
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "type",
                throwExceptions = true,
            )
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByScopeAddressOrNull(
                scopeAddress = "randomscopeaddress",
                assetType = "type",
            )
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
            suite.querier.queryAssetScopeAttributeByScopeAddress(
                scopeAddress = "address",
                assetType = "type",
            )
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetScopeAttributeByScopeAddress(
                scopeAddress = "address",
                assetType = "type",
            )
        }
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(attribute)
        val attributeFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributeByScopeAddress(
                scopeAddress = "randomscopeaddress",
                assetType = "type",
            )
        }
        assertEquals(
            expected = attribute,
            actual = attributeFromQuery,
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryAssetScopeAttributesByAssetUuidOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do all scope attributes things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryAssetScopeAttributesByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                throwExceptions = true,
            )
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetScopeAttributesByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                throwExceptions = false,
            )
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetScopeAttributesByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                throwExceptions = true,
            )
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(listOf(attribute))
        val attributesFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributesByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
            )
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = 1,
            actual = attributesFromQuery.size,
            message = "Expected a single attribute to be returned via the query",
        )
        assertEquals(
            expected = attribute,
            actual = attributesFromQuery.single(),
            message = "Expected the attribute returned from the query to match the scope attribute's initial value"
        )
    }

    @Test
    fun `test queryAssetScopeAttributesByAssetUuid`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do scope attribute things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryAssetScopeAttributesByAssetUuid(
                assetUuid = UUID.randomUUID(),
            )
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetScopeAttributesByAssetUuid(
                assetUuid = UUID.randomUUID(),
            )
        }
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(listOf(attribute))
        val attributesFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributesByAssetUuid(
                assetUuid = UUID.randomUUID(),
            )
        }
        assertEquals(
            expected = 1,
            actual = attributesFromQuery.size,
            message = "Expected a single attribute to be returned via the query",
        )
        assertEquals(
            expected = attribute,
            actual = attributesFromQuery.single(),
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryAssetScopeAttributesByScopeAddressOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do all scope attributes things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryAssetScopeAttributesByScopeAddressOrNull(
                scopeAddress = "address",
                throwExceptions = true,
            )
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryAssetScopeAttributesByScopeAddressOrNull(
                scopeAddress = "address",
                throwExceptions = false,
            )
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryAssetScopeAttributesByScopeAddressOrNull(
                scopeAddress = "address",
                throwExceptions = true,
            )
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(listOf(attribute))
        val attributesFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributesByScopeAddressOrNull(
                scopeAddress = "address",
            )
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = 1,
            actual = attributesFromQuery.size,
            message = "Expected a single attribute to be returned via the query",
        )
        assertEquals(
            expected = attribute,
            actual = attributesFromQuery.single(),
            message = "Expected the attribute returned from the query to match the scope attribute's initial value"
        )
    }

    @Test
    fun `test queryAssetScopeAttributesByScopeAddress`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do scope attribute things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryAssetScopeAttributesByScopeAddress(
                scopeAddress = "address",
            )
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryAssetScopeAttributesByScopeAddress(
                scopeAddress = "address",
            )
        }
        val attribute = suite.mockScopeAttribute()
        suite.mockQueryReturns(listOf(attribute))
        val attributesFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryAssetScopeAttributesByScopeAddress(
                scopeAddress = "address",
            )
        }
        assertEquals(
            expected = 1,
            actual = attributesFromQuery.size,
            message = "Expected a single attribute to be returned via the query",
        )
        assertEquals(
            expected = attribute,
            actual = attributesFromQuery.single(),
            message = "Expected the output to match the scope attribute's initial value",
        )
    }

    @Test
    fun `test queryFeePaymentsByAssetUuidOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do fee payment things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryFeePaymentsByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
                throwExceptions = true,
            )
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryFeePaymentsByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
                throwExceptions = false,
            )
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryFeePaymentsByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
                throwExceptions = true,
            )
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val feePayments = suite.mockFeePaymentDetail()
        suite.mockQueryReturns(feePayments)
        val feePaymentsFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryFeePaymentsByAssetUuidOrNull(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
                throwExceptions = true,
            )
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = feePayments,
            actual = feePaymentsFromQuery,
            message = "Expected the output to match the fee payments initial value",
        )
    }

    @Test
    fun `test queryFeePaymentsByAssetUuid`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do fee payment things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryFeePaymentsByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
            )
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryFeePaymentsByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
            )
        }
        val feePayments = suite.mockFeePaymentDetail()
        suite.mockQueryReturns(feePayments)
        val feePaymentsFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryFeePaymentsByAssetUuid(
                assetUuid = UUID.randomUUID(),
                assetType = "heloc",
            )
        }
        assertEquals(
            expected = feePayments,
            actual = feePaymentsFromQuery,
            message = "Expected the output to match the fee payments initial value",
        )
    }

    @Test
    fun `test queryFeePaymentsByScopeAddressOrNull`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do fee payment things"))
        assertThrows<IllegalStateException>("Expected an exception to be rethrown when requested") {
            suite.querier.queryFeePaymentsByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "heloc",
                throwExceptions = true,
            )
        }
        assertSucceeds("Expected no exception to be thrown when throwExceptions is disabled") {
            suite.querier.queryFeePaymentsByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "heloc",
                throwExceptions = false,
            )
        }.assertNull("Expected the response value to be null on a successful no-exceptions run")
        suite.mockQueryNullResponse()
        assertSucceeds("Expected no exception to be thrown when the contract returns a null response") {
            suite.querier.queryFeePaymentsByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "heloc",
                throwExceptions = true,
            )
        }.assertNull("Expected the response value to be null when the contract returns a null response")
        val feePayments = suite.mockFeePaymentDetail()
        suite.mockQueryReturns(feePayments)
        val feePaymentsFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryFeePaymentsByScopeAddressOrNull(
                scopeAddress = "address",
                assetType = "heloc",
                throwExceptions = true,
            )
        }.assertNotNull("Expected the response to be non-null when a value is returned")
        assertEquals(
            expected = feePayments,
            actual = feePaymentsFromQuery,
            message = "Expected the output to match the fee payments initial value",
        )
    }

    @Test
    fun `test queryFeePaymentsByScopeAddress`() {
        val suite = MockSuite.new()
        suite.mockQueryThrows(IllegalStateException("Failed to do fee payment things"))
        assertThrows<IllegalStateException>("Expected an exception to be thrown when encountered") {
            suite.querier.queryFeePaymentsByScopeAddress(
                scopeAddress = "address",
                assetType = "heloc",
            )
        }
        suite.mockQueryNullResponse()
        assertThrows<NullContractResponseException>("Expected a null contract response exception when the contract responds with null") {
            suite.querier.queryFeePaymentsByScopeAddress(
                scopeAddress = "address",
                assetType = "heloc",
            )
        }
        val feePayments = suite.mockFeePaymentDetail()
        suite.mockQueryReturns(feePayments)
        val feePaymentsFromQuery = assertSucceeds("Expected the query to execute successfully when the proper response is returned") {
            suite.querier.queryFeePaymentsByScopeAddress(
                scopeAddress = "address",
                assetType = "heloc",
            )
        }
        assertEquals(
            expected = feePayments,
            actual = feePaymentsFromQuery,
            message = "Expected the output to match the fee payments initial value",
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
                every { pbClient.nameClient.resolve(any()) } returns QueryResolveResponse.newBuilder().setAddress(
                    DEFAULT_CONTRACT_ADDRESS
                ).build()
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
            displayName = assetType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
        )

        fun mockScopeAttribute(): AssetScopeAttribute = AssetScopeAttribute(
            assetUuid = UUID.randomUUID(),
            scopeAddress = "randomscopeaddress",
            assetType = "heloc",
            requestorAddress = "requestor",
            verifierAddress = "verifier",
            onboardingStatus = AssetOnboardingStatus.APPROVED,
            latestVerificationResult = AssetVerificationResult(
                message = "Validation was pretty good on this here scope",
                success = true,
            ),
            accessDefinitions = emptyList(),
        )

        fun mockFeePaymentDetail(): FeePaymentDetail = FeePaymentDetail(
            scopeAddress = "randomscopeaddress",
            assetType = "heloc",
            payments = listOf(
                FeePayment(
                    amount = Coin.newBuilder().setAmount("100").setDenom("nhash").build(),
                    name = "Verifier payment",
                    recipient = "verifieraddress",
                ),
                FeePayment(
                    amount = Coin.newBuilder().setAmount("10").setDenom("nhash").build(),
                    name = "Admin fee",
                    recipient = "adminaddress",
                )
            )
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
