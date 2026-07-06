package elieoko.app.mcoresystem.data.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val DAILY_WORK = "mcore_daily_reminder"
    private const val STALE_WORK = "mcore_stale_operation_reminder"

    fun schedule(context: Context) {
        NotificationHelper.ensureChannel(context)
        val workManager = WorkManager.getInstance(context)

        val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayToHour(9), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

        val staleRequest = PeriodicWorkRequestBuilder<StaleOperationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayToHour(18), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            STALE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            staleRequest
        )
    }

    private fun initialDelayToHour(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
