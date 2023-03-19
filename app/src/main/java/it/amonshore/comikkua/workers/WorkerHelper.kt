package it.amonshore.comikkua.workers

import android.app.Activity
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

fun createUpdateReleasesWorker() =
    OneTimeWorkRequest.Builder(UpdateReleasesWorkerKt::class.java).build()

fun <A> enqueueUpdateReleasesWorker(
    activity: A,
    onSuccess: Consumer<Data>,
    onFail: Runnable
)
        where A : Activity,
              A : LifecycleOwner {
    enqueueUpdateReleasesWorker(activity, onSuccess::accept, onFail::run)
}

fun <A> enqueueUpdateReleasesWorker(
    activity: A,
    onSuccess: (Data) -> Unit,
    onFail: () -> Unit
)
        where A : Activity,
              A : LifecycleOwner {
    val request = createUpdateReleasesWorker()
    val workManager = WorkManager.getInstance(activity)
    workManager.enqueue(request)
    workManager.getWorkInfoByIdLiveData(request.id).observe(activity) {
        when (it.state) {
            WorkInfo.State.SUCCEEDED -> onSuccess(it.outputData)
            WorkInfo.State.FAILED,
            WorkInfo.State.BLOCKED,
            WorkInfo.State.CANCELLED -> onFail()
            else -> {}
        }
    }
}