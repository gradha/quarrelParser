import es.elhaso.quarrelparser.QuarrelParser
import es.elhaso.quarrelparser.QuarrelParser.ParamKind
import es.elhaso.quarrelparser.QuarrelParser.ParameterSpecification
import es.elhaso.quarrelparser.println
import platform.posix.exit

// Example defining a simple copy command line program.

const val PARAM_PRESERVE_ALL = "-a"
const val PARAM_FORCE = "-f"
const val PARAM_FOLLOW_SOME_SYMBOLIC_LINKS = "-H"
const val PARAM_INTERACTIVE = "-i"
const val PARAM_FOLLOW_ALL_SYMBOLIC_LINKS = "-L"
const val PARAM_NO_OVERWRITE = "-n"
const val PARAM_FOLLOW_NO_SYMBOLIC_LINKS = "-P"
const val PARAM_PRESERVE_ATTRIBUTES = "-p"
const val PARAM_RECURSIVE = "-R"
const val PARAM_VERBOSE = "-v"
const val PARAM_HELP = "-h"


fun processCommandline(args: List<String>): QuarrelParser.CommandlineResults {
    val params = mutableListOf<ParameterSpecification>()

    fun p(inputParam: String, help: String) {
        params.add(ParameterSpecification(listOf(inputParam), helpText = help))
    }
    p(
        PARAM_PRESERVE_ALL,
        ("Same as $PARAM_PRESERVE_ATTRIBUTES $PARAM_FOLLOW_NO_SYMBOLIC_LINKS " +
                "$PARAM_RECURSIVE options, preserves structure and " +
                "attributes of files but not directory structure")
    )
    p(PARAM_FORCE, "Force overwrite destination files")
    p(
        PARAM_FOLLOW_SOME_SYMBOLIC_LINKS,
        "Follow symbolic links on the command line"
    )
    p(PARAM_INTERACTIVE, "Prompt before overwritting destination")
    p(PARAM_FOLLOW_ALL_SYMBOLIC_LINKS, "Follow all symbolic links recursively")
    p(PARAM_NO_OVERWRITE, "Do not overwrite destination")
    p(PARAM_FOLLOW_NO_SYMBOLIC_LINKS, "No symbolic links are followed")
    p(PARAM_PRESERVE_ATTRIBUTES, "Attributes are preserved to destination")
    p(PARAM_RECURSIVE, "Follow source directories recursively")
    p(PARAM_VERBOSE, "Be verbose about actions")

    params.add(
        ParameterSpecification(
            names = listOf(PARAM_HELP),
            consumes = ParamKind.Help,
            helpText = "Shows this help on the commandline"
        )
    )

    val result = QuarrelParser.parse(args, params)

    if (result.positionalParameters.size < 2) {
        println("Missing parameters, you need to pass the source and dest targets.")
        params.println()
        exit(0)
    }

    fun check(param: String) {
        if (result.options.containsKey(param)) println("Found option '$param'.")
    }

    check(PARAM_PRESERVE_ALL)
    check(PARAM_FORCE)
    check(PARAM_FOLLOW_SOME_SYMBOLIC_LINKS)
    check(PARAM_INTERACTIVE)
    check(PARAM_FOLLOW_ALL_SYMBOLIC_LINKS)
    check(PARAM_NO_OVERWRITE)
    check(PARAM_FOLLOW_NO_SYMBOLIC_LINKS)
    check(PARAM_PRESERVE_ATTRIBUTES)
    check(PARAM_RECURSIVE)
    check(PARAM_VERBOSE)

    return result
}

fun main(args: Array<String>) {
    val allArgs = args.joinToString(", ")
    println(" Multiplatform CLI: ${getWorld()} with args $allArgs")
    println(" ----- ")
    val parsedArgs = processCommandline(args.toList())
    val dest = parsedArgs.positionalParameters.last()
    for (i in 0 until parsedArgs.positionalParameters.size - 1) {
        val src = parsedArgs.positionalParameters[i].strVal
        println("Copying $src -> ${dest.strVal}")
    }
}
