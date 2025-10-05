package es.elhaso.quarrelparser


/** Parses a string into a boolean value or throws.
 *
 * @return true if the string is `y`, `yes`, `true`, `1` or `on`.
 *
 * false if the string is `n`, `no`, `false`, `0` or `off`.
 *
 * Throws [QuarrelParseError] if the string doesn't match any of the previous values.
 */
public fun String.toQuarrelBoolean(): Boolean = toQuarrelBooleanOrNull()
    ?: throw QuarrelParseError("Cannot interpret '$this' as a bool")

/** Parses a string into a boolean value or null.
 *
 * @return true if the string is `y`, `yes`, `true`, `1` or `on`.
 *
 * false if the string is `n`, `no`, `false`, `0` or `off`.
 *
 * null if the string doesn't match any of the previous values.
 */
public fun String.toQuarrelBooleanOrNull(): Boolean? = when (this) {
    "y", "yes", "true", "1", "on" -> true
    "n", "no", "false", "0", "off" -> false
    else -> null
}