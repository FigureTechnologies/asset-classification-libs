package tech.figure.classification.asset.client.client.extension

fun buildLogMessage(msg: String, args: List<Pair<String, Any?>>): String {
    val argString = args.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
    ) { "${it.first}=${it.second}" }

    return "$msg $argString".trim()
}

fun buildLogMessage(msg: String, vararg args: Pair<String, Any?>): String = buildLogMessage(msg, args.toList())
