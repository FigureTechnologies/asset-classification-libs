package testconfiguration.containers

import org.testcontainers.containers.GenericContainer

interface ManagedTestContainer<out T : GenericContainer<out T>> {
    val containerType: ManagedContainerType

    fun buildContainer(): T
    fun afterStartup(container: @UnsafeVariance T) {}
}
