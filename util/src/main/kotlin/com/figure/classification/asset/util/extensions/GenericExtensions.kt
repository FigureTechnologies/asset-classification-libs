package com.figure.classification.asset.util.extensions

/**
 * All extensions in this library are suffixed with "Ac" to ensure they do not overlap with other libraries' extensions.
 */

/**
 * The functional equivalent of an elvis operator, allowing default values to be derived when an elvis operator
 * would cause weird-looking syntax, like parenthetical enclosures that unnecessarily obfuscate meaning.
 */
fun <T> T?.elvisAc(default: () -> T): T = this ?: default()

/**
 * Functionally identical to Kotlin Stdlib's .also extension function, except that the logic will only execute if the
 * condition value is passed as 'true'.
 */
fun <T> T.alsoIfAc(condition: Boolean, action: () -> Unit): T = this.also {
    if (condition) {
        action.invoke()
    }
}

fun <T> T.wrapListAc(): List<T> = listOf(this)

fun <T> T.wrapSetAc(): Set<T> = setOf(this)

inline fun <reified T> T.wrapArrayAc(): Array<T> = arrayOf(this)
