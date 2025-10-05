package es.elhaso.quarrelparser

import es.elhaso.quarrelparser.QuarrelParser.ParamKind
import es.elhaso.quarrelparser.shared.V_MAJOR
import es.elhaso.quarrelparser.shared.V_MINOR
import es.elhaso.quarrelparser.shared.V_PATCH
import kotlin.experimental.ExperimentalNativeApi

/** Prototype of parameter callbacks.
 *
 * A parameter callback is just a custom function you provide which is invoked
 * after a parameter is parsed passing the basic type validation. The
 * first parameter is the string which triggered the option. The
 * second parameter contains the [QuarrelParser.ParsedParameter] already parsed
 * into the basic type you specified for it.
 *
 * The callback fun has to decide to change the input `value` or not. If
 * nothing is to be done, your fun can simply pass back again the `inputParameter`.
 * But if you want to change it, you can return any new custom value. In fact, if you
 * need special parsing, most likely you will end up specifying
 * [QuarrelParser.ParamKind.String] in the parameter input specification so
 * that the [QuarrelParser.parse] function doesn't
 * *mangle* the string before you can process it yourself.
 *
 * If the callback decides to abort the validation of the parameter, can
 * simply throw any exception.
 */
typealias ParameterCallback = (parameter: String, inputParameter: QuarrelParser.ParsedParameter) -> QuarrelParser.ParsedParameter

open class QuarrelError(message: String? = null) : Exception(message)
class QuarrelParseError(message: String) : QuarrelError(message)
class QuarrelMissingParamError(message: String) : QuarrelError(message)


class QuarrelParser {

    /**
     * Module version as an integer tuple.
     *
     * Major versions changes mean a break in API backwards compatibility, either
     * through removal of symbols or modification of their purpose.
     *
     * Minor version changes can add procs (and maybe default parameters).
     *
     * Maintenance version changes mean bugfixes or non API changes.
     *
     * Patch odd versions are development/git/unstable versions. Patch even versions
     * are public stable releases.
     */
    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
    )

    /** Different types of results for parameter parsing
     */
    enum class ParamKind {
        Empty, Int, Long, Float, Double, String, Boolean, Help;
    }

    /** Holds the expectations of a parameter.
     *
     * You create these objects and feed them to the parse() proc, which then
     * uses them to detect parameters and turn them into something useful.
     */
    data class ParameterSpecification(
        /** List of possible parameters to catch for this. */
        val names: List<String>,
        /** Expected type of the parameter ([QuarrelParser.ParamKind.Empty] for none). */
        val consumes: ParamKind = ParamKind.Empty,
        /** Optional custom callback to run after type conversion. See [ParameterCallback]. */
        val customValidator: ParameterCallback? = null,
        /** Help for this group of parameters. */
        val helpText: String = "",
    )

    /** Contains the parsed value from the user.
     *
     * Implemented as a sealed class, you can use `when` to deal with parsed parameters,
     * or you can cast the [ParsedParameter] to the type you expect if you only handle one.
     */
    sealed class ParsedParameter {
        class ParsedEmpty : ParsedParameter()
        class ParsedHelp : ParsedParameter()
        data class ParsedInt(val value: Int) : ParsedParameter()
        data class ParsedLong(val value: Long) : ParsedParameter()
        data class ParsedFloat(val value: Float) : ParsedParameter()
        data class ParsedDouble(val value: Double) : ParsedParameter()
        data class ParsedString(val value: String) : ParsedParameter()
        data class ParsedBoolean(val value: Boolean) : ParsedParameter()

        val strVal: String
            get() {
                return (this as ParsedString).value
            }

        val intVal: Int
            get() {
                return (this as ParsedInt).value
            }

        val longVal: Long
            get() {
                return (this as ParsedLong).value
            }

        val floatVal: Float
            get() {
                return (this as ParsedFloat).value
            }

        val doubleVal: Double
            get() {
                return (this as ParsedDouble).value
            }

        val booleanVal: Boolean
            get() {
                return (this as ParsedBoolean).value
            }

    }

    /** Contains the results of the parsing.
     *
     * Usually this is the result of the parse() call, but you can add extension
     * functions to it for accessor convenience.
     *
     * Note that you always have to access the [QuarrelParser.CommandlineResults.options]
     * ordered table with the first variant of a parameter name. For instance, if you have an
     * option specified like ``["-s", "--silent"]`` and the user types
     * ``--silent`` at the commandline, you have to use
     * ``options.hasKey("-s")`` to test for it. This standarizes access through
     * the first name variant for all options to avoid you repeating the test
     * with different keys.
     */
    data class CommandlineResults(
        val positionalParameters: List<ParsedParameter>,
        val options: Map<String, ParsedParameter>,
    )

    companion object {

        val version: Version = Version(
            major = V_MAJOR,
            minor = V_MINOR,
            patch = V_PATCH
        )

        const val DEFAULT_END_OPTIONS = "--"
        val DEFAULT_BAD_PREFIXES = listOf("-", "--")

        /** Parses parameters and returns results.
         *
         * If there is any kind of error during parsing an exception will be raised.
         *
         * @param args list of parameters passed to your program
         * without the program binary (usually OSes provide the path to the binary as
         * the zeroth parameter).
         *
         * @param expected list of the parameters you want to
         * detect, which can capture additional values. Uncaptured parameters are
         * considered positional parameters for which you can specify a type with
         * [kindOfPositionalParameters].
         *
         * @param kindOfPositionalParameters the [ParamKind] for positional parameters.
         * By default [ParamKind.String].
         *
         * @param badPrefixes Before accepting a positional parameter, the list of
         * [badPrefixes] is compared against it. If the positional parameter starts
         * with any of them, an error is displayed to the user due to ambiguity.
         * See [DEFAULT_BAD_PREFIXES].
         *
         * @param endOfOptions The user can overcome
         * the ambiguity by typing the special string specified by [endOfOptions].
         * Note that values captured by parameters are not checked against bad
         * prefixes, otherwise it would be a problem to specify the dash as synonym
         * for standard input for many programs. See [DEFAULT_END_OPTIONS].
         */
        @OptIn(ExperimentalNativeApi::class)
        fun parse(
            args: List<String>,
            expected: List<ParameterSpecification> = listOf(),
            kindOfPositionalParameters: ParamKind = ParamKind.String,
            badPrefixes: List<String> = DEFAULT_BAD_PREFIXES,
            endOfOptions: String = DEFAULT_END_OPTIONS,
        ): CommandlineResults {

            assert(
                !(kindOfPositionalParameters == ParamKind.Empty ||
                        kindOfPositionalParameters == ParamKind.Help)
            ) {
                "kindOfPositionalParameters can't be $kindOfPositionalParameters"
            }

            badPrefixes.forEach {
                assert(it.isNotEmpty()) { "badPrefixes can't contain zero length strings" }
            }

            val lookup = buildSpecificationLookup(expected)
            var addingOptions = true
            val positionalParameters = ArrayList<ParsedParameter>(args.size)
            val options = HashMap<String, ParsedParameter>(args.size)

            // Loop through the input arguments detecting their type and doing stuff.
            var i = 0
            while (i < args.size) {
                val arg = args[i]

                if (arg.isNotEmpty() && addingOptions) {
                    if (arg == endOfOptions) {
                        // Looks like we found the end_of_options marker, disable options.
                        addingOptions = false
                        i++
                        continue
                    } else if (lookup.containsKey(arg)) {
                        val param = lookup[arg]!!

                        // Insert check here for help, which aborts parsing.
                        if (param.consumes == ParamKind.Help) {
                            echoHelp(
                                expected, kindOfPositionalParameters,
                                badPrefixes, endOfOptions
                            )
                            throw IllegalStateException("Eh, abort due to help?")
                        }

                        val parsed: ParsedParameter = if (param.consumes != ParamKind.Empty) {

                            if (i + 1 >= args.size) {
                                throw QuarrelMissingParamError(
                                    "Parameter '$arg' requires a value, but none was provided"
                                )
                            }

                            val parsed = parseParameter(
                                param = arg,
                                value = args[i + 1],
                                paramKind = param.consumes
                            )
                            i++
                            if (param.customValidator != null)
                                param.customValidator(arg, parsed)
                            else
                                parsed
                        } else {
                            ParsedParameter.ParsedEmpty()
                        }

                        options[param.names[0]] = parsed
                        i++
                        continue
                    } else {
                        // Add the parameter as positional, but check if it is confusing.
                        badPrefixes.forEach { badPrefix ->
                            if (arg.startsWith(badPrefix))
                                throw IllegalArgumentException(
                                    "Found ambiguous parameter '$arg' starting with '$badPrefix', " +
                                            "put '$endOfOptions' as the previous parameter " +
                                            "if you want to force it as positional parameter."
                                )
                        }
                    }
                }

                // Unprocessed, add the parameter to the list of positional parameters.
                positionalParameters.add(
                    parseParameter(
                        param = (1 + i).toString(),
                        value = arg,
                        paramKind = kindOfPositionalParameters,
                    )
                )
                i++
            }

            return CommandlineResults(
                positionalParameters = positionalParameters,
                options = options,
            )
        }
    }
}

private fun buildSpecificationLookup(
    expected: List<QuarrelParser.ParameterSpecification>
): Map<String, QuarrelParser.ParameterSpecification> {

    val result = LinkedHashMap<String, QuarrelParser.ParameterSpecification>(expected.size * 2)

    for (i in 0 until expected.size) {
        for (paramToDetect in expected[i].names) {
            if (result.containsKey(paramToDetect)) {
                throw IllegalArgumentException(
                    "Parameter '$paramToDetect' repeated in input specification"
                )
            } else {
                result[paramToDetect] = expected[i]
            }
        }
    }

    return result
}

/** Tries to parse a text according to the specified type.
 *
 * Pass the parameter string which requires a value and the text the user
 * passed in for it. It will be parsed according to the param_kind. This proc
 * will raise (ValueError, OverflowError) if something can't be parsed.
 */
private fun parseParameter(
    param: String,
    value: String,
    paramKind: QuarrelParser.ParamKind
): QuarrelParser.ParsedParameter {

    return when (paramKind) {
        ParamKind.Empty -> QuarrelParser.ParsedParameter.ParsedEmpty()
        ParamKind.Int -> QuarrelParser.ParsedParameter.ParsedInt(
            value.toIntOrNull() ?: throw QuarrelParseError(
                "Param '$param' with value '$value' can't be parsed into integer."
            )
        )

        ParamKind.Long -> QuarrelParser.ParsedParameter.ParsedLong(
            value.toLongOrNull() ?: throw QuarrelParseError(
                "Param '$param' with value '$value' can't be parsed into a long."
            )
        )

        ParamKind.Float -> QuarrelParser.ParsedParameter.ParsedFloat(
            value.toFloatOrNull() ?: throw QuarrelParseError(
                "Param '$param' with value '$value' can't be parsed into a float."
            )
        )

        ParamKind.Double -> QuarrelParser.ParsedParameter.ParsedDouble(
            value.toDoubleOrNull() ?: throw QuarrelParseError(
                "Param '$param' with value '$value' can't be parsed into a double."
            )
        )

        ParamKind.String -> QuarrelParser.ParsedParameter.ParsedString(value)
        ParamKind.Boolean -> QuarrelParser.ParsedParameter.ParsedBoolean(
            value.toQuarrelBooleanOrNull() ?: throw QuarrelParseError(
                "Param '$param' with value '$value' can't be parsed into a boolean."
            )
        )

        ParamKind.Help -> QuarrelParser.ParsedParameter.ParsedHelp()
    }
}

/* Builds basic help text and returns it as a sequence of strings.
 *
 * Note that this proc doesn't do as much sanity checks as the normal parse()
 * proc, though it's unlikely you will be using one without the other, so if
 * you had a parameter specification problem you would find out soon.
 */
fun buildHelp(
    expected: List<QuarrelParser.ParameterSpecification> = emptyList(),
    kindOfPositionalParameters: ParamKind = ParamKind.String,
    badPrefixes: List<String> = QuarrelParser.DEFAULT_BAD_PREFIXES,
    endOfOptions: String = QuarrelParser.DEFAULT_END_OPTIONS,
): List<String> {
    val result = mutableListOf("Usage parameters: ")

    // Generate lookup table for each type of parameter based on strings.
    val lookup = buildSpecificationLookup(expected)
    val keys = lookup.keys.toList()
    val prefixes = mutableListOf<String>()
    val helps = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    // First generate the joined version of input parameters in a list.
    for (key in keys) {
        if (seen.contains(key))
            continue

        // Add the joined string to the list
        val param = lookup[key]!!
        val param_names = param.names.sorted()
        var prefix = param_names.joinToString(", ")
        // Don't forget about the type, if the parameter consumes values
        if (param.consumes != ParamKind.Empty && param.consumes != ParamKind.Help)
            prefix = "$prefix ${param.consumes}"

        prefixes.add(prefix)
        helps.add(param.helpText)
        // Ignore future elements
        param.names.forEach { seen.add(it) }
    }

    // Calculate the biggest width and try to use that
    val width = prefixes.maxOf { 3 + it.length }

    prefixes.zip(helps).forEach { (prefix, help) ->
        result.add(prefix.padEnd(width - prefix.length) + " $help")
    }

    return result
}

private fun echoHelp(
    expected: List<QuarrelParser.ParameterSpecification>,
    kindOfPositionalParameters: ParamKind,
    badPrefixes: List<String>,
    endOfOptions: String,
) {
    buildHelp(expected, kindOfPositionalParameters, badPrefixes, endOfOptions).forEach { line ->
        println(line)
    }
}