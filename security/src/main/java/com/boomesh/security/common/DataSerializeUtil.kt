package com.boomesh.security.common

internal object DataSerializeUtil {
    private const val STRING_SET_PREFIX = "string_set["
    private const val STRING_SET_SUFFIX = "]"
    private const val STRING_SET_DELIM = ","
    private const val BOOLEAN_PREFIX = "boolean("
    private const val PRIMITIVE_SUFFIX = ")"

    internal fun stringToStringSet(string: String): MutableSet<String> {
        return string
            .removePrefix(STRING_SET_PREFIX)
            .removeSuffix(STRING_SET_SUFFIX)
            .split(STRING_SET_DELIM)
            .toMutableSet()
    }

    internal fun stringSetToString(set: Set<String>): String {
        return set.joinToString(
            separator = STRING_SET_DELIM,
            prefix = STRING_SET_PREFIX,
            postfix = STRING_SET_SUFFIX
        )
    }

    internal fun isStringSet(string: String): Boolean {
        return string.startsWith(STRING_SET_PREFIX) && string.endsWith(STRING_SET_SUFFIX)
    }

    internal fun stringToBoolean(string: String): Boolean {
        return string.removePrefix(BOOLEAN_PREFIX).removeSuffix(
            PRIMITIVE_SUFFIX
        ).toBoolean()
    }

    internal fun booleanToString(boolean: Boolean): String {
        return "$BOOLEAN_PREFIX$boolean$PRIMITIVE_SUFFIX"
    }

    internal fun isBoolean(string: String): Boolean {
        return string.startsWith(BOOLEAN_PREFIX) && string.endsWith(PRIMITIVE_SUFFIX)
    }
}