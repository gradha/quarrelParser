import es.elhaso.quarrelParser.QuarrelMissingParamError
import es.elhaso.quarrelParser.QuarrelParseError
import es.elhaso.quarrelParser.QuarrelParser
import es.elhaso.quarrelParser.QuarrelParser.Companion.parse
import es.elhaso.quarrelParser.QuarrelParser.ParamKind
import es.elhaso.quarrelParser.QuarrelParser.ParameterSpecification
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ApiTests {

    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun test() {
        val p1 = ParameterSpecification(listOf("-a"), ParamKind.String)
        val p2 = ParameterSpecification(listOf("--aasd"), ParamKind.String)
        val p3 = ParameterSpecification(listOf("-i"), ParamKind.Int)
        val p4 = ParameterSpecification(listOf("-f"), ParamKind.Float)
        val p5 = ParameterSpecification(listOf("-b"), ParamKind.Boolean)
        val p6 = ParameterSpecification(listOf("-I"), ParamKind.Long)
        val p7 = ParameterSpecification(listOf("-F"), ParamKind.Double)
        val allParams = listOf(p1, p2, p3, p4, p5, p6, p7)

        val exerciseParser = parse(listOf("foo", "bar"), allParams)
        println("Parsed $exerciseParser")

        val args = listOf("test", "toca me la", "-a", "-wo", "rd", "--aasd", "--s", "ugh")
        var ret = parse(args, allParams)
        println("Got $ret")
        assert(ret.options["-a"]?.strVal == "-wo")
        assert(ret.options.containsKey("test") == false)
        assert(ret.testPositional("test"))
        assert(ret.testPositional("--s") == false)
        assert(ret.testPositional("ugh"))
        assertFailsWith(QuarrelParseError::class) { parse(args, allParams, ParamKind.Int) }

        // Integer tests
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
        ret = parse(listOf("-i", "-445", "2", "3", "4"), allParams, ParamKind.Int)
        assertFailsWith(QuarrelParseError::class) {
            parse(listOf("-i", "-445", "2", "3", "4.3"), allParams, ParamKind.Int)
        }
        assert(ret.options["-i"]?.intVal == -445)
        assert(ret.testPositional(2))
        assert(ret.testPositional(3))
        assert(ret.testPositional(4))
        assert(ret.testPositional(5) == false)

        // String tests
        parse(listOf("str test", "-a", "word"), allParams)
        parse(listOf("str empty test", "-a", ""), allParams)
        assertFailsWith(QuarrelMissingParamError::class) {
            parse(listOf("str test", "-a"), allParams)
        }

        // Float tests.
        parse(listOf("-f", "123.235"), allParams)
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-f", ""), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-f", "abracadabra"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-f", "12.34aadd"), allParams) }
        ret = parse(listOf("-f", "12.23", "89.2", "3.14"), allParams, ParamKind.Float)
        assert(ret.options["-f"]?.floatVal == 12.23f)
        assert(ret.testPositional(89.2f))
        assert(ret.testPositional(3.14f))
        assert(ret.testPositional(3.1f) == false)

        // Boolean tests.
        listOf("y", "yes", "true", "1", "on", "n", "no", "false", "0", "off").forEach {
            parse(listOf("-b", it), allParams)
        }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-b", "t"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-b", ""), allParams) }
        parse(listOf("y"), allParams, ParamKind.Boolean).testPositional(true)
        parse(listOf("0"), allParams, ParamKind.Boolean).testPositional(false)

        // Big integer tests.
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
        ret = parse(listOf("42", Long.MAX_VALUE.toString()), allParams, ParamKind.Long)
        assert(ret.testPositional(42L))
        assert(ret.testPositional(Long.MAX_VALUE))
        assert(ret.testPositional(13L) == false)

        // Double tests.
        parse(listOf("-F", "123.235"), allParams)
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-F", ""), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-F", "abracadabra"), allParams) }
        assertFailsWith(QuarrelParseError::class) { parse(listOf("-F", "12.34aadd"), allParams) }
        ret = parse(listOf("111.111", "9.01"), allParams, ParamKind.Double)
        assert(ret.testPositional(111.111))
        assert(ret.testPositional(9.01))
        assert(ret.testPositional(9.02) == false)
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