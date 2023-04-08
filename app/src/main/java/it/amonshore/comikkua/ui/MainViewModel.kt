package it.amonshore.comikkua.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.work.*
import it.amonshore.comikkua.*
import it.amonshore.comikkua.R
import it.amonshore.comikkua.workers.ReleasesNotificationWorker
import it.amonshore.comikkua.workers.UpdateReleasesWorker
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _onAutoUpdateEnabledChanged =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == Constants.KEY_AUTO_UPDATE_ENABLED) {
                val enabled = sharedPreferences.getBoolean(Constants.KEY_AUTO_UPDATE_ENABLED, false)
                LogHelperKt.d { "${Constants.KEY_AUTO_UPDATE_ENABLED} is changed to $enabled" }
                if (enabled) {
                    enqueueUpdateReleasesWorker(application)
                } else {
                    cancelUpdateReleasesWorker(application)
                }
            }
        }

    init {
        if (BuildConfig.DEBUG) {
            WorkManager.getInstance(application).cancelAllWork()
        }

        _sharedPreferences.registerOnSharedPreferenceChangeListener(
            _onAutoUpdateEnabledChanged
        )
    }

    override fun onCleared() {
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            _onAutoUpdateEnabledChanged
        )
    }

    fun setupWorkers() {
        LogHelperKt.d { "Setup workers" }
        setupUpdateReleasesWorker()
        setupReleaseNotificationWorker()
    }

    private fun setupUpdateReleasesWorker() {
        val context: Context = getApplication()

        val notificationChannel = NotificationChannel(
            UpdateReleasesWorker.CHANNEL_ID,
            context.getString(R.string.auto_update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.auto_update_channel_description) }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)

        val enabled = _sharedPreferences.getBoolean(Constants.KEY_AUTO_UPDATE_ENABLED, false)
        if (enabled) {
            enqueueUpdateReleasesWorker(context)
        } else {
            cancelUpdateReleasesWorker(context)
        }
    }

    private fun enqueueUpdateReleasesWorker(context: Context) {
        LogHelperKt.d { "Enqueue UpdateReleasesWorker" }
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request =
            PeriodicWorkRequest.Builder(UpdateReleasesWorker::class.java, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            UpdateReleasesWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancelUpdateReleasesWorker(context: Context) {
        LogHelperKt.d { "Cancel UpdateReleasesWorker" }
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UpdateReleasesWorker.WORK_NAME)
    }

    private fun setupReleaseNotificationWorker() = viewModelScope.launch {
        val context: Context = getApplication()

        val notificationChannel = NotificationChannel(
            ReleasesNotificationWorker.CHANNEL_ID,
            context.getString(R.string.today_releases_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.today_releases_channel_description) }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)

        if (notificationManager.areNotificationsEnabled()) {
            enqueueReleaseNotificationWorker(context)
        } else {
            cancelReleaseNotificationWorker(context)
        }
    }

    private fun enqueueReleaseNotificationWorker(context: Context) {
        LogHelperKt.d { "Enqueue ReleaseNotificationWorker" }
        val workManager = WorkManager.getInstance(context)

        val nextStart = LocalDateTime.now().next(LocalTime.of(8, 0, 0))
        val delay = Duration.ofMillis(LocalDateTime.now().until(nextStart, ChronoUnit.MILLIS))

        LogHelperKt.d { "ReleaseNotificationWorker initial delay=$delay" }

        val request =
            PeriodicWorkRequest.Builder(ReleasesNotificationWorker::class.java, 1, TimeUnit.DAYS)
                .setInitialDelay(delay)
                .build()

        workManager.enqueueUniquePeriodicWork(
            ReleasesNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancelReleaseNotificationWorker(context: Context) {
        LogHelperKt.d { "Cancel ReleaseNotificationWorker" }
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(ReleasesNotificationWorker.WORK_NAME)
    }
}