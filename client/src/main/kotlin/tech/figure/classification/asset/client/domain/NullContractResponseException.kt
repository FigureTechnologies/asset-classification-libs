package tech.figure.classification.asset.client.domain

/**
 * Returned when the contract returns a null response, but the current call does not expect null responses.
 */
data class NullContractResponseException(
    override val message: String,
    val t: Throwable? = null
) : Exception(message, t)
