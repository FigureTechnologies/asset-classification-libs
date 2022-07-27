package testconfiguration.containers.instances

import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import testconfiguration.containers.ManagedContainerType
import testconfiguration.containers.ManagedTestContainer

class ManagedPostgresTestContainer : ManagedTestContainer<PostgreSQLContainerOverride> {
    override val containerType: ManagedContainerType = ManagedContainerType.POSTGRES

    override fun buildContainer(network: Network): PostgreSQLContainerOverride = PostgreSQLContainerOverride("postgres:13-alpine")
        .withNetwork(network)
        .withNetworkMode(network.id)
        .withNetworkAliases("postgres")
        .withDatabaseName("object-store")
        .withUsername("postgres")
        .withPassword("password1")
}

class PostgreSQLContainerOverride(imageName: String) : PostgreSQLContainer<PostgreSQLContainerOverride>(imageName) {
    override fun configure() {
        super.configure()
        setCommand("postgres", "-c", "integrationtest.safe=1", "-c", "fsync=off")
    }
}
