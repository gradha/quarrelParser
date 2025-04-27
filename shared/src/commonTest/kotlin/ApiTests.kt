import es.elhaso.quarrelParser.QuarrelMissingParamError
import es.elhaso.quarrelParser.QuarrelParseError
import es.elhaso.quarrelParser.QuarrelParser
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

        val exerciseParser = QuarrelParser.parse(listOf("foo", "bar"), allParams)
        println("Parsed $exerciseParser")

        val args = listOf("test", "toca me la", "-a", "-wo", "rd", "--aasd", "--s", "ugh")
        var ret = QuarrelParser.parse(args, allParams)
        println("Got $ret")
        assert(ret.options["-a"]?.strVal == "-wo")
        assert(ret.options.containsKey("test") == false)
        assert(ret.testPositional("test"))
        assert(ret.testPositional("--s") == false)
        assert(ret.testPositional("ugh"))
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(args, allParams, kindOfPositionalParameters = ParamKind.Int)
        }

        // Integer tests
        QuarrelParser.parse(listOf("int test", "-i", "445"), allParams)
        QuarrelParser.parse(listOf("int test", "-i", "-445"), allParams)
        assertFailsWith(QuarrelMissingParamError::class) {
            QuarrelParser.parse(listOf("-i"), allParams)
        }
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-i", "0x02"), allParams)
        }
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-i", "fail"), allParams)
        }
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-i", "234.12"), allParams)
        }
        // TODO: Instead of QuarrelParseError, create specific overflow exception.
        // This requires custom parsing though
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-i", Int.MAX_VALUE.toString() + "0"), allParams)
        }
        ret = QuarrelParser.parse(
            args = listOf("-i", "-445", "2", "3", "4"),
            expected = allParams,
            kindOfPositionalParameters = ParamKind.Int
        )
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(
                args = listOf("-i", "-445", "2", "3", "4.3"),
                expected = allParams,
                kindOfPositionalParameters = ParamKind.Int
            )
        }
        assert(ret.options["-i"]?.intVal == -445)
        assert(ret.testPositional(2))
        assert(ret.testPositional(3))
        assert(ret.testPositional(4))
        assert(ret.testPositional(5) == false)

        // String tests
        QuarrelParser.parse(listOf("str test", "-a", "word"), allParams)
        QuarrelParser.parse(listOf("str empty test", "-a", ""), allParams)
        assertFailsWith(QuarrelMissingParamError::class) {
            QuarrelParser.parse(listOf("str test", "-a"), allParams)
        }

        // Float tests.
        QuarrelParser.parse(listOf("-f", "123.235"), allParams)
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-f", ""), allParams)
        }
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-f", "abracadabra"), allParams)
        }
        assertFailsWith(QuarrelParseError::class) {
            QuarrelParser.parse(listOf("-f", "12.34aadd"), allParams)
        }
        ret = QuarrelParser.parse(
            args = listOf("-f", "12.23", "89.2", "3.14"),
            expected = allParams,
            kindOfPositionalParameters = ParamKind.Float
        )
        assert(ret.options["-f"]?.floatVal == 12.23f)
        assert(ret.testPositional(89.2f))
        assert(ret.testPositional(3.14f))
        assert(ret.testPositional(3.1f) == false)

        

    }
}

private inline fun <reified T> QuarrelParser.CommandlineResults.testPositional(value: T): Boolean {
    this.positionalParameters.forEach {
        when (T::class) {
            String::class -> if (it.strVal == value) return true
            Int::class -> if (it.intVal == value) return true
            Float::class -> if (it.floatVal == value) return true
            else -> throw IllegalArgumentException("Unimplemented for $value")
        }
    }
    return false
}