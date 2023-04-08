package it.amonshore.comikkua

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class EchoWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        return when (inputData.size()) {
            0 -> Result.failure()
            else -> Result.success(inputData)
        }
    }
}

class ListenableEchoWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        delay(500)
        return when (inputData.size()) {
            0 -> Result.failure()
            else -> Result.success(inputData)
        }
    }
}

@RunWith(AndroidJUnit4::class)
class WorkerTest {
    private lateinit var context: Context
    private lateinit var executor: Executor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
    }

    @Test
    fun test_echo_worker() {
        val data = workDataOf("A" to 1)

        val worker = TestWorkerBuilder<EchoWorker>(
            context = context,
            executor = executor,
            inputData = data

        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(ListenableWorker.Result.success(data)))
    }

    @Test
    fun test_listenable_echo_worker() {
        val data = workDataOf("A" to 1)

        val worker = TestListenableWorkerBuilder<ListenableEchoWorker>(
            context = context,
            inputData = data

        ).build()

        runBlocking {
            val result = worker.doWork()
            assertThat(result, `is`(ListenableWorker.Result.success(data)))
        }
    }
}