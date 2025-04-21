import kotlin.test.Test

private inline fun scope(block: () -> Unit) {
    block()
}

class FooTest {

    @Test
    fun identityTest() {
        println("Dummy")
    }

}