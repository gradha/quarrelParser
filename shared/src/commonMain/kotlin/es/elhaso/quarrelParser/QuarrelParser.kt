package es.elhaso.quarrelParser

import es.elhaso.quarrelParser.shared.V_MAJOR
import es.elhaso.quarrelParser.shared.V_MINOR
import es.elhaso.quarrelParser.shared.V_PATCH

typealias ParameterCallback = (parameter: String, value: String) -> String

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

    companion object {
        val version: Version = Version(
            major = V_MAJOR,
            minor = V_MINOR,
            patch = V_PATCH
        )
    }

    /** Different types of results for parameter parsing
     */
    enum class ParamKind { Empty, Int, Long, Float, Double, String, Boolean, Help }

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
        val helpText: String,
    )

    /** Contains the parsed value from the user.
     *
     * Implemented as a sealed class, you can use `when` to deal with parsed parameters,
     * or you can cast the [ParsedParameter] to the type you expect if you only handle one.
     */
    sealed class ParsedParameter {
        class ParsedEmpty
        class ParsedHelp
        data class ParsedInt(val value: Int)
        data class ParsedLong(val value: Long)
        data class ParsedFloat(val value: Float)
        data class ParsedDouble(val value: Double)
        data class ParsedString(val value: String)
        data class ParsedBoolean(val value: Boolean)
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

    

}