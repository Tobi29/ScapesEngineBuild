package org.tobi29.scapes.engine.gradle

fun String.splitArgumentList(): List<String> {
    val output = ArrayList<String>()
    val current = StringBuilder()

    var i = 0
    while (i < length) {
        val c = this[i]
        when (c) {
            '\\' -> {
                if (i + 1 >= length) {
                    throw IllegalArgumentException(
                            "Backslash without escaped character")
                }
                val c1 = this[i + 1]
                when (c1) {
                    '\\', ',' -> current.append(c1)
                    else -> throw IllegalArgumentException(
                            "Invalid escaped character: $c1")
                }
            }
            ',' -> {
                output.add(current.toString())
                current.delete(0, current.length)
            }
            else -> current.append(c)
        }
        i++
    }
    output.add(current.toString())

    return output
}

fun String.splitArgumentMap(): Map<String, String> {
    val output = HashMap<String, String>()
    val currentKey = StringBuilder()
    val currentValue = StringBuilder()
    var current = currentKey

    var i = 0
    while (i < length) {
        val c = this[i]
        when (c) {
            '\\' -> {
                if (i + 1 >= length) {
                    throw IllegalArgumentException(
                            "Backslash without escaped character")
                }
                val c1 = this[i + 1]
                when (c1) {
                    '\\', ',', '=' -> current.append(c1)
                    else -> throw IllegalArgumentException(
                            "Invalid escaped character: $c1")
                }
            }
            '=' -> if (current === currentKey) {
                current = currentValue
            } else {
                current.append(c)
            }
            ',' -> {
                output[currentKey.toString()] = currentValue.toString()
                currentKey.delete(0, currentKey.length)
                currentValue.delete(0, currentValue.length)
                current = currentKey
            }
            else -> current.append(c)
        }
        i++
    }
    output[currentKey.toString()] = currentValue.toString()

    return output
}
