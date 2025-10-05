import es.elhaso.quarrelparser.QuarrelMissingParamError
import es.elhaso.quarrelparser.QuarrelParseError
import es.elhaso.quarrelparser.QuarrelParser
import es.elhaso.quarrelparser.QuarrelParser.CommandlineResults
import es.elhaso.quarrelparser.QuarrelParser.Companion.DEFAULT_BAD_PREFIXES
import es.elhaso.quarrelparser.QuarrelParser.Companion.DEFAULT_END_OPTIONS
import es.elhaso.quarrelparser.QuarrelParser.ParamKind
import es.elhaso.quarrelparser.QuarrelParser.ParameterSpecification
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.test.assertFailsWith

private fun parse(
    args: List<String>,
    expected: List<ParameterSpecification> = listOf(),
    kindOfPositionalParameters: ParamKind = ParamKind.String,
    badPrefixes: List<String> = DEFAULT_BAD_PREFIXES,
    endOfOptions: String = DEFAULT_END_OPTIONS,
): CommandlineResults =
    QuarrelParser.parse(
        args = args,
        expected = expected,
        kindOfPositionalParameters = kindOfPositionalParameters,
        badPrefixes = badPrefixes,
        endOfOptions = endOfOptions,
        quitOnFailure = false,
    )

@OptIn(ExperimentalNativeApi::class)
class ApiTests {

    val p1 = ParameterSpecification(listOf("-a"), ParamKind.String)
    val p2 = ParameterSpecification(listOf("--aasd"), ParamKind.String)
    val p3 = ParameterSpecification(listOf("-i"), ParamKind.Int)
    val p4 = ParameterSpecification(listOf("-f"), ParamKind.Float)
    val p5 = ParameterSpecification(listOf("-b"), ParamKind.Boolean)
    val p6 = ParameterSpecification(listOf("-I"), ParamKind.Long)
    val p7 = ParameterSpecification(listOf("-F"), ParamKind.Double)
    val allParams = listOf(p1, p2, p3, p4, p5, p6, p7)

    @Test
    fun `Parse test`() {
        val exerciseParser = parse(listOf("foo", "bar"), allParams)
        println("Parsed $exerciseParser")
        assert(exerciseParser.positionalParameters.size == 2)
    }

    @Test
    fun `Positional tests`() {
        val args = listOf("test", "toca me la", "-a", "-wo", "rd", "--aasd", "--s", "ugh")
        val ret = parse(args, allParams)
        println("Got $ret")
        assert(ret.options["-a"]?.strVal == "-wo")
        assert(ret.options.containsKey("test") == false)
        assert(ret.testPositional("test"))
        assert(ret.testPositional("--s") == false)
        assert(ret.testPositional("ugh"))
        assertFailsWith(QuarrelParseError::class) { parse(args, allParams, ParamKind.Int) }
    }

    @Test
    fun `Integer tests`() {
        parse(listOf("int test", "-i", "445"), allParams)
        parse(listOf("int test", "-i", "-445"), allParams)
        assertFailsWith(QuarrelMissingParamError::class) { parse(listOf("-i"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-i", "0x02"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-i", "fail"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-i", "234.12"), allParams) }
        // TODO: Instead of QuarrelParseError, create specific overflow exception.
        // This requires custom parsing though
        assertFailsWith(QuarrelParseError::class) {
            parse(listOf("-i", Int.MAX_VALUE.toString() + "0"), allParams)
        }
        val ret = parse(listOf("-i", "-445", "2", "3", "4"), allParams, ParamKind.Int)
        assertFailsWith(QuarrelParseError::class) {
            parse(listOf("-i", "-445", "2", "3", "4.3"), allParams, ParamKind.Int)
        }
        assert(ret.options["-i"]?.intVal == -445)
        assert(ret.testPositional(2))
        assert(ret.testPositional(3))
        assert(ret.testPositional(4))
        assert(ret.testPositional(5) == false)
    }

    @Test
    fun `String tests`() {
        parse(listOf("str test", "-a", "word"), allParams)
        parse(listOf("str empty test", "-a", ""), allParams)
        assertFailsWith(QuarrelMissingParamError::class) {
            parse(listOf("str test", "-a"), allParams)
        }
    }

    @Test
    fun `Float tests`() {
        parse(listOf("-f", "123.235"), allParams)
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-f", ""), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-f", "abracadabra"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-f", "12.34aadd"), allParams) }
        val ret = parse(listOf("-f", "12.23", "89.2", "3.14"), allParams, ParamKind.Float)
        assert(ret.options["-f"]?.floatVal == 12.23f)
        assert(ret.testPositional(89.2f))
        assert(ret.testPositional(3.14f))
        assert(ret.testPositional(3.1f) == false)
    }

    @Test
    fun `Boolean tests`() {
        listOf("y", "yes", "true", "1", "on", "n", "no", "false", "0", "off").forEach {
            parse(listOf("-b", it), allParams)
        }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-b", "t"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-b", ""), allParams) }
        parse(listOf("y"), allParams, ParamKind.Boolean).testPositional(true)
        parse(listOf("0"), allParams, ParamKind.Boolean).testPositional(false)

        // Try using now the second version of the switches
        val boolArgs = listOf("file1", "--silent")
        var res = parse(boolArgs, listOf(ParameterSpecification(listOf("-s", "--silent"))))
        assert(res.options.containsKey("-s"))
        assert(!res.options.containsKey("--silent"))

        res = parse(boolArgs, listOf(ParameterSpecification(listOf("--silent", "-s"))))
        assert(!res.options.containsKey("-s"))
        assert(res.options.containsKey("--silent"))
    }

    @Test
    fun `Long tests`() {
        parse(listOf("int test", "-I", "445"), allParams)
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-I", ""), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-I", "fail"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-I", "234.12"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-I", "234.12"), allParams) }
        // TODO: Instead of QuarrelParseError, create specific overflow exception.
        // This requires custom parsing though
        assertFailsWith(QuarrelParseError::class) {
            parse(listOf("-I", Long.MAX_VALUE.toString() + "0"), allParams)
        }
        val ret = parse(listOf("42", Long.MAX_VALUE.toString()), allParams, ParamKind.Long)
        assert(ret.testPositional(42L))
        assert(ret.testPositional(Long.MAX_VALUE))
        assert(ret.testPositional(13L) == false)
    }

    @Test
    fun `Double tests`() {
        parse(listOf("-F", "123.235"), allParams)
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-F", ""), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-F", "abracadabra"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-F", "12.34aadd"), allParams) }
        val ret = parse(listOf("111.111", "9.01"), allParams, ParamKind.Double)
        assert(ret.testPositional(111.111))
        assert(ret.testPositional(9.01))
        assert(ret.testPositional(9.02) == false)
    }

    @Test
    fun `Using custom procs for transformation of type back to string`() {
        val c1 = ParameterSpecification(listOf("-i"), ParamKind.Int)
        var ret = parse(listOf("-i", "42"), listOf(c1))
        assert(ret.options["-i"]?.intVal == 42)

        val c2 = c1.copy(
            customValidator = { parameter: String, inputParameter: QuarrelParser.ParsedParameter ->
                QuarrelParser.ParsedParameter.ParsedString(inputParameter.intVal.toString())
            }
        )
        ret = parse(listOf("-i", "42"), listOf(c2))
        assert(ret.options["-i"]?.strVal == "42")
    }

    @Test
    fun `Use a custom callback to reject values lower than 18`() {
        val c1 = ParameterSpecification(listOf("-i"), ParamKind.Int)
        var ret = parse(listOf("-i", "42"), listOf(c1))
        assert(ret.options["-i"]?.intVal == 42)

        val c3 = c1.copy(
            customValidator = { parameter: String, inputParameter: QuarrelParser.ParsedParameter ->
                val age = inputParameter.intVal
                if (age < 18)
                    throw IllegalArgumentException("Can't accept minors ($age) passing arguments")
                QuarrelParser.ParsedParameter.ParsedString("valid_${inputParameter.intVal}")
            }
        )
        ret = parse(listOf("-i", "42"), listOf(c3))
        assert(ret.options["-i"]?.strVal == "valid_42")
        assertFailsWith(IllegalArgumentException::class) { parse(listOf("-i", "17"), listOf(c3)) }
    }

    @Test
    fun `Wrong parser input conditions`() {
        // Disallow multiple parameters being the same
        val c1 = ParameterSpecification(listOf("-a", "-a"))
        assertFailsWith(IllegalArgumentException::class) { parse(listOf(), listOf(c1)) }

        // Also disallow multiple parameters being the same in different options
        val c2 = ParameterSpecification(listOf("-a", "--alt"))
        val c3 = ParameterSpecification(listOf("-p", "--pert"))
        val c4 = ParameterSpecification(listOf("-z", "--alt"))
        assertFailsWith(IllegalArgumentException::class) { parse(listOf(), listOf(c2, c3, c4)) }

        // Ambiguous parameters
        assertFailsWith(IllegalArgumentException::class) {
            parse(listOf("-bleah", "something"), allParams)
        }
        // This will work because we are using the disambiguator
        parse(listOf("--", "-bleah", "something"), allParams)
        // This should work too because we changed the pad prefixes
        parse(listOf("-bl", "so"), allParams, badPrefixes = listOf())
        // This should detect new prefixes
        assertFailsWith(IllegalArgumentException::class) {
            parse(listOf("/bl", "so"), allParams, badPrefixes = listOf("/"))
        }
        // Mix new prefixes plus end of parsing options.
        parse(listOf("-*-", "/bĸ", "a"), allParams, badPrefixes = listOf("/"), endOfOptions = "-*-")
    }

}

private inline fun <reified T> QuarrelParser.CommandlineResults.testPositional(value: T): Boolean {
    this.positionalParameters.forEach {
        when (T::class) {
            String::class -> if (it.strVal == value) return true
            Int::class -> if (it.intVal == value) return true
            Float::class -> if (it.floatVal == value) return true
            Double::class -> if (it.doubleVal == value) return true
            Boolean::class -> if (it.booleanVal == value) return true
            Long::class -> if (it.longVal == value) return true
            else -> throw IllegalArgumentException("Unimplemented for $value")
        }
    }
    return false
}