package it.amonshore.comikkua

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CoroutineTest {

    private fun runLongTaskAndCancel() = runBlocking {
        try {
            // Arrange
            val size = 10
            val data = 1..size
            val calls = data.map {
                async {
                    println("exec $it")
                    delay(50L)
                    it
                }
            }

            // Act
            delay(500L)
            println("cancel...")
            cancel("it was me")

            println("await all...")
            val res = calls.awaitAll()

            res.size
        } catch (ex: CancellationException) {
            println("runLongTaskAndCancel catching cancel exception")
            throw ex
        }
    }

    @Test
    fun cancel_async_throws_error() {
        assertThrows("it was me", CancellationException::class.java) {
            runLongTaskAndCancel()
        }
    }

    @Test
    fun avoiding_cancel_async_throws_error() {
        try {
            runLongTaskAndCancel()
        } catch (_: CancellationException) {
            println("task canceled")
        }
    }

    private suspend fun exec() {
        for (i in 0..100) {
            delay(100L)
            println("exec $i")
        }
    }

    @Test
    fun cancel_suspend() = runBlocking {
        exec()

        println("cancel...")
        Thread {
            Thread.sleep(1000L)
            cancel("it was me")
        }.start()

        println("wait...")
        delay(10000L)
    }
}

