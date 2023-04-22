package it.amonshore.comikkua

import org.junit.Test
import org.junit.Assert.*

class ContextReceiversTest {

    inner class MyContext {
        fun count() = 10
    }

    context(MyContext)
    private fun exec(): Int {
        return count()
    }

    @Test
    fun with_context() {
        val context = MyContext()
        val count = with(context) {
            exec()
        }
        assertEquals(10, count)
    }
}