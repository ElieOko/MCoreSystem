package elieoko.app.mcoresystem

import android.app.*
import elieoko.app.mcoresystem.data.notification.ReminderScheduler
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.data.preferences.SessionRepository
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.data.remote.NetworkMonitor
import elieoko.app.mcoresystem.data.remote.SyncManager
import elieoko.app.mcoresystem.data.remote.SyncWorker
import elieoko.app.mcoresystem.data.room.*
import elieoko.app.mcoresystem.domain.repository.AuthRepository
import elieoko.app.mcoresystem.domain.repository.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MCoreApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { MCoreRoomDatabase.getDatabase(this, applicationScope) }
    private val changeTracker by lazy { ChangeTracker(database.syncQueueDao()) }

    val userRepository by lazy { UserRepository(database.userDao()) }
    val paymentMethodRepository by lazy { PaymentMethodRepository(database.paymentMethodDao(), changeTracker) }
    val currencyRepository by lazy { CurrencyRepository(database.currencyDao(), changeTracker) }
    val categoryRepository by lazy { CategorieRepository(database.categoryDao(), changeTracker) }
    val typeCategoryRepository by lazy { TypeCategorieRepository(database.typeCategoryDao(), changeTracker) }
    val operationRepository by lazy { OperationRepository(database.operationDao(), changeTracker) }
    val organismRepository by lazy { OrganismRepository(database.organismDao()) }
    val exchangeRateRepository by lazy { ExchangeRateRepository(this) }
    val sessionRepository by lazy { SessionRepository(this) }
    val syncManager by lazy { SyncManager(database) }
    val authRepository by lazy { AuthRepository(database, sessionRepository, syncManager) }

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.schedule(this)
        SyncWorker.schedule(this)
        // Synchronisation automatique dès que la connexion revient.
        networkMonitor = NetworkMonitor(this) { triggerSync() }
    }

    fun triggerSync() {
        applicationScope.launch {
            try {
                val session = sessionRepository.session.first()
                if (session.isLoggedIn && session.organismUuid.isNotBlank()) {
                    syncManager.syncAll(session.organismUuid)
                }
            } catch (_: Exception) {
                // La synchronisation réessaiera au prochain déclencheur.
            }
        }
    }
}
