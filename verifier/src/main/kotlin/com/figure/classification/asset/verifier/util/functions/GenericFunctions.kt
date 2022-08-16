package com.figure.classification.asset.verifier.util.functions

/**
 * Allows a try/catch to be inlined in a much more concise way, simply hiding the emitted exception by returning a
 * default value of null.
 */
internal fun <T> tryOrNull(block: () -> T): T? = try { block() } catch (e: Exception) { null }

/**
 * The same as tryOrNull, but for suspending functions.
 */
internal suspend fun <T> tryOrNullAsync(block: suspend () -> T): T? = try { block() } catch (e: Exception) { null }
