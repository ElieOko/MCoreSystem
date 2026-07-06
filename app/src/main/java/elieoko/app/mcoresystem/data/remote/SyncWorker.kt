package elieoko.app.mcoresystem.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import elieoko.app.mcoresystem.MCoreApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Synchronisation périodique en arrière-plan (uniquement lorsque le réseau est disponible).
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MCoreApplication ?: return Result.success()
        return try {
            val session = app.sessionRepository.session.first()
            if (session.isLoggedIn && session.organismUuid.isNotBlank()) {
                app.syncManager.syncAll(session.organismUuid)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "mcore_background_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
