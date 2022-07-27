package testconfiguration.containers.waitstrategy

import io.grpc.ConnectivityState
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import testconfiguration.util.CoroutineUtil
import java.util.concurrent.TimeUnit

class GrpcWaitStrategy : AbstractWaitStrategy() {
    private companion object : KLogging()

    private var serverPort: Int = 8080
    private var connectionRetryMinPollMs: Long = 1000L
    private var connectionRetryMaxPollMs: Long = 10000L
    private var connectionRetryCount: Int = 10
    private var verboseRetryLogs: Boolean = false
    private var useGrpcSecure: Boolean = false
    private var acceptableConnectivityStates: Set<ConnectivityState> = setOf(ConnectivityState.READY)

    fun withServerPort(port: Int): GrpcWaitStrategy = apply { this.serverPort = port }
    fun withConnectionRetryMinPollMs(minMs: Long): GrpcWaitStrategy = apply { this.connectionRetryMinPollMs = minMs }
    fun withConnectionRetryMaxPollMs(maxMs: Long): GrpcWaitStrategy = apply { this.connectionRetryMaxPollMs = maxMs }
    fun withConnectionRetryCount(count: Int): GrpcWaitStrategy = apply { this.connectionRetryCount = count }
    fun withVerboseRetryLogs(verbose: Boolean): GrpcWaitStrategy = apply { this.verboseRetryLogs = verbose }
    fun withGrpcSecure(secure: Boolean): GrpcWaitStrategy = apply { this.useGrpcSecure = secure }
    fun withAcceptableConnectivityStates(states: Set<ConnectivityState>) = apply { this.acceptableConnectivityStates = states }

    override fun waitUntilReady() {
        try {
            val host = waitStrategyTarget.host
            val port = waitStrategyTarget.getMappedPort(serverPort)
            logger.info("Opening a channel for GRPC communication with container [${waitStrategyTarget.containerInfo.name}] on [$host:$port (mapped from port $serverPort)]")
            val channel = ManagedChannelBuilder.forAddress(host, port)
                .let { builder ->
                    if (useGrpcSecure) {
                        builder.useTransportSecurity()
                    } else {
                        builder.usePlaintext()
                    }
                }
                .idleTimeout(60, TimeUnit.SECONDS)
                .keepAliveTime(10, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build()
            logger.info("Attempting to fetch server state...")
            // Attempt to fetch the connectivity status - this will blow up if the container is not available
            runBlocking {
                launch {
                    val serverState = CoroutineUtil.withRetryBackoff(
                        errorPrefix = "Waiting for target server to be in one of the following states: $acceptableConnectivityStates",
                        initialDelay = connectionRetryMinPollMs,
                        maxDelay = connectionRetryMaxPollMs,
                        times = connectionRetryCount,
                        showStackTraceInFailures = verboseRetryLogs,
                        block = {
                            channel.getState(true).also { state ->
                                check(state in acceptableConnectivityStates) { "Expected the server to be in one of the states $acceptableConnectivityStates, but it had a state of [$state]" }
                            }
                        }
                    )
                    logger.info("Target server successfully started and has connectivity state [$serverState]")
                }.join()
            }
            logger.info("Shutting down GRPC communication channel...")
            channel.shutdown()
            logger.info("Successfully shutdown grpc connection to container [${waitStrategyTarget.containerInfo.name}] on [$host:$port (mapped from port $serverPort)]")
        } catch (e: Exception) {
            throw ContainerLaunchException("GRPC connection to container [${waitStrategyTarget.containerInfo.name}] was not available", e)
        }
    }
}
