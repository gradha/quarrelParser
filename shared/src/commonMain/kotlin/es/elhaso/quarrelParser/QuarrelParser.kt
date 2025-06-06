package es.elhaso.quarrelParser

import es.elhaso.quarrelParser.QuarrelParser.ParamKind
import es.elhaso.quarrelParser.shared.V_MAJOR
import es.elhaso.quarrelParser.shared.V_MINOR
import es.elhaso.quarrelParser.shared.V_PATCH
import kotlin.experimental.ExperimentalNativeApi

typealias ParameterCallback = (parameter: String, value: String) -> String

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
        val consumes: ParamKind,
        /** Optional custom callback to run after type conversion. */
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
         * @param badPrefixes Before accepting a positional parameter, the list of
         * [badPrefixes] is compared against it. If the positional parameter starts
         * with any of them, * an error is displayed to the user due to ambiguity.
         *
         * @param endOfOptions The user can overcome
         * the ambiguity by typing the special string specified by [endOfOptions].
         * Note that values captured by parameters are not checked against bad
         * prefixes, otherwise it would be a problem to specify the dash as synonym
         * for standard input for many programs.
         */
        @OptIn(ExperimentalNativeApi::class)
        fun parse(
            args: List<String>,
            expected: List<ParameterSpecification> = listOf(),
            kindOfPositionalParameters: ParamKind = ParamKind.String,
            badPrefixes: List<String> = listOf("-", "--"),
            endOfOptions: String = "--",
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
                            // TODO: Run custom proc
                            i++
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

private fun echoHelp(
    expected: List<QuarrelParser.ParameterSpecification> = emptyList(),
    kindOfPositionalParameters: ParamKind = ParamKind.String,
    badPrefixes: List<String> = listOf("-", "--"),
    endOfOptions: String = "--",
) {
    println("Print help to terminal, TODO")
}