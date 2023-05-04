package testconfiguration.containers

enum class ManagedContainerType(val displayName: String) {
    OBJECT_STORE("Object Store"),
    POSTGRES("PostgreSQL"),
    PROVENANCE("Provenance")
}
