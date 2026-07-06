package elieoko.app.mcoresystem

import android.app.*
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.data.room.*
import elieoko.app.mcoresystem.domain.repository.room.*
import kotlinx.coroutines.*

class MCoreApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { MCoreRoomDatabase.getDatabase(this, applicationScope) }
    val userRepository by lazy { UserRepository(database.userDao()) }
    val paymentMethodRepository by lazy { PaymentMethodRepository(database.paymentMethodDao()) }
    val currencyRepository by lazy { CurrencyRepository(database.currencyDao()) }
    val categoryRepository by lazy { CategorieRepository(database.categoryDao()) }
    val typeCategoryRepository by lazy { TypeCategorieRepository(database.typeCategoryDao()) }
    val operationRepository by lazy { OperationRepository(database.operationDao()) }
    val organismRepository by lazy { OrganismRepository(database.organismDao()) }
    val exchangeRateRepository by lazy { ExchangeRateRepository(this) }


}
//    override fun onTrimMemory(level: Int) {
//        super.onTrimMemory(level)
//    }