import es.elhaso.quarrelParser.QuarrelParser
import es.elhaso.quarrelParser.QuarrelParser.ParamKind
import es.elhaso.quarrelParser.QuarrelParser.ParameterSpecification
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test

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
        assert(ret.options["-a"]?.strVal == "-wo" )
        assert(ret.options.containsKey("test") == false)
        //doAssert test_in(ret, "test", str_val)
        //doAssert test_in(ret, "--s", str_val) == false
        //doAssert test_in(ret, "ugh", str_val)
        //test_failure(ValueError, tp(all_params, PK_INT, args))


    }
}