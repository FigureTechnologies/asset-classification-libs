package testconfiguration.containers

import mu.KLogging
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class ContainerRegistry(private val network: Network) {
    private companion object : KLogging()
    private var containerOrderCounter = 0
    private val containerMap: MutableMap<ManagedContainerType, OrderedContainer> = mutableMapOf()

    fun registerAndStart(container: ManagedTestContainer<*>) {
        if (containerMap.containsKey(container.containerType)) {
            error("A container of type [${container.containerType.displayName}] has already been registered")
        }
        val builtContainer = container.buildContainer(network)
        try {
            builtContainer.start()
        } catch (e: Exception) {
            logger.error("Failed to start container of type: ${container.containerType.displayName}]", e)
        }
        containerMap += container.containerType to OrderedContainer(
            order = containerOrderCounter,
            managedContainer = container,
            container = builtContainer,
        )
        container.afterStartup(builtContainer)
        containerOrderCounter ++
    }

    fun getContainer(containerType: ManagedContainerType): GenericContainer<*> = containerMap[containerType]
        ?.container
        ?: error("No container of type [${containerType.displayName}] has been registered")

    fun getOrderedContainers(): List<GenericContainer<*>> = containerMap
        .values
        .sortedBy { it.order }
        .map { it.container }
}

private data class OrderedContainer(
    val order: Int,
    val managedContainer: ManagedTestContainer<*>,
    val container: GenericContainer<*>,
)

