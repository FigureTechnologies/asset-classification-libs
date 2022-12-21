package tech.figure.classification.asset.client.client.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.provenance.client.grpc.PbClient
import tech.figure.classification.asset.client.client.base.ACClient
import tech.figure.classification.asset.client.client.base.ACExecutor
import tech.figure.classification.asset.client.client.base.ACQuerier

/**
 * The default implementation for an [ACClient].  Allows the client to be a composition of its various elements.
 * Use [ACClient.getDefault] to retrieve an instance of this.
 */
class DefaultACClient(
    override val pbClient: PbClient,
    override val objectMapper: ObjectMapper,
    private val executor: ACExecutor,
    private val querier: ACQuerier,
) : ACClient, ACExecutor by executor, ACQuerier by querier
