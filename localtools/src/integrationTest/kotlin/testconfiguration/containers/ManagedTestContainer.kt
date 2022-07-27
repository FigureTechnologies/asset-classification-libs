package testconfiguration.containers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

interface ManagedTestContainer<out T : GenericContainer<out T>> {
    val containerType: ManagedContainerType

    fun buildContainer(network: Network): T
    fun afterStartup(container: @UnsafeVariance T) {}
}
