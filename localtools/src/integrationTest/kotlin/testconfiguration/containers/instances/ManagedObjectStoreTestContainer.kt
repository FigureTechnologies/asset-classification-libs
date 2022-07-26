package testconfiguration.containers.instances

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import testconfiguration.containers.ManagedContainerType
import testconfiguration.containers.ManagedTestContainer
import testconfiguration.containers.waitstrategy.GrpcWaitStrategy

class ManagedObjectStoreTestContainer : ManagedTestContainer<ObjectStoreTestContainer> {
    override val containerType: ManagedContainerType = ManagedContainerType.OBJECT_STORE

    override fun buildContainer(network: Network): ObjectStoreTestContainer = ObjectStoreTestContainer(grpcPort = 5000)
        .withNetwork(network)
        .withNetworkMode(network.id)
        .withNetworkAliases("object-store")
        .waitingFor(GrpcWaitStrategy().withServerPort(5000).withConnectionRetryCount(5))
}

class ObjectStoreTestContainer(val grpcPort: Int) : GenericContainer<ObjectStoreTestContainer>("ghcr.io/provenance-io/object-store:0.7.0") {
    override fun configure() {
        addEnv("OS_URL", "0.0.0.0")
        addEnv("OS_PORT", grpcPort.toString())
        addEnv("DB_CONNECTION_POOL_SIZE", "10")
        addEnv("DB_HOST", "postgres")
        addEnv("DB_USER", "postgres")
        addEnv("DB_PASS", "password1")
        addEnv("DB_PORT", "5432")
        addEnv("DB_NAME", "object-store")
        addEnv("DB_SCHEMA", "public")
        addEnv("URI_HOST", "$host:$grpcPort")
        addEnv("STORAGE_TYPE", "file_system")
        addEnv("STORAGE_BASE_PATH", "/mnt/data")
        addEnv("RUST_LOG", "warn,object-store=debug")
        addEnv("TRACE_HEADER", "x-trace-header")
        addEnv("LOGGING_THRESHOLD_SECONDS", "1")
        addExposedPort(grpcPort)
    }
}
