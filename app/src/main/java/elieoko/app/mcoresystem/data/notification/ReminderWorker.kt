package elieoko.app.mcoresystem.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import elieoko.app.mcoresystem.MCoreApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Rappel quotidien de saisie, uniquement les jours ouvrables (lundi au samedi).
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Dimanche exclu : on ne notifie que du lundi au samedi.
        if (dayOfWeek == Calendar.SUNDAY) return Result.success()

        NotificationHelper.notify(
            applicationContext,
            NOTIFICATION_ID,
            title = "Saisie du jour",
            message = "N'oubliez pas d'enregistrer les opérations du jour dans MCoreSystem."
        )
        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}

/**
 * Rappel pour les opérations laissées non clôturées depuis plus de 5 jours.
 * Exécuté une fois par jour (ne dérange donc pas de façon répétée).
 */
class StaleOperationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? MCoreApplication ?: return@withContext Result.success()
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return@withContext Result.success()

        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = Date()
        val staleCount = runCatching {
            app.operationRepository.pendingOperations().count { relation ->
                val createdOn = relation.operation?.createdOn ?: return@count false
                val created = runCatching { format.parse(createdOn) }.getOrNull() ?: return@count false
                val diffDays = (now.time - created.time) / (1000L * 60 * 60 * 24)
                diffDays >= STALE_DAYS
            }
        }.getOrDefault(0)

        if (staleCount > 0) {
            val message = if (staleCount == 1) {
                "1 opération n'a pas été clôturée depuis plus de $STALE_DAYS jours."
            } else {
                "$staleCount opérations n'ont pas été clôturées depuis plus de $STALE_DAYS jours."
            }
            NotificationHelper.notify(
                applicationContext,
                NOTIFICATION_ID,
                title = "Opérations en attente",
                message = message
            )
        }
        Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val STALE_DAYS = 5
    }
}
