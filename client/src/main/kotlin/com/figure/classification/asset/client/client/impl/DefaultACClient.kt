package com.figure.classification.asset.client.client.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.figure.classification.asset.client.client.base.ACClient
import com.figure.classification.asset.client.client.base.ACExecutor
import com.figure.classification.asset.client.client.base.ACQuerier
import io.provenance.client.grpc.PbClient

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
