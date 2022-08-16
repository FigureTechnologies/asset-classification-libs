package com.figure.classification.asset.localtools.extensions

internal fun <T : Any> T?.checkNotNullAc(lazyMessage: () -> String): T {
    checkNotNull(this, lazyMessage)
    return this
}

internal fun <T> tryOrNullAc(fn: () -> T): T? = try { fn() } catch (e: Exception) { null }

internal fun <T : Any?, U : Any?, R> T.combineWithAc(that: U, transform: (T, U) -> R): R = transform(this, that)
