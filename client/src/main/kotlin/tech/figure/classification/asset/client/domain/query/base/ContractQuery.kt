package tech.figure.classification.asset.client.domain.query.base

import tech.figure.classification.asset.client.domain.ContractAction

/**
 * A simple interface that should be tagged on all contract queries.  This prevents invalid classes from being used
 * as query targets to keep the code honest.
 */
interface ContractQuery : ContractAction {
    /**
     * Allows the query to define a message when a null response is returned by the contract.  Improves client
     * consumer experience with a more readable response upon failure.
     */
    val queryFailureMessage: String
}
