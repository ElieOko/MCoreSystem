package elieoko.app.mcoresystem.data.room

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.interfaces.room.*
import elieoko.app.mcoresystem.domain.model.room.*
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@Database(
    entities = [CurrencyModel::class, PaymentMethodModel::class, OperationModel::class, CategoryModel::class, TypeCategoryModel::class, OrganismModel::class, UserModel::class], version = 1, exportSchema = false)
abstract class MCoreRoomDatabase : RoomDatabase() {
    abstract fun currencyDao(): ICurrencyDao
    abstract fun paymentMethodDao(): IPaymentMethodDao
    abstract fun userDao(): IUserDao
    abstract fun operationDao(): IOperationDao
    abstract fun organismDao(): IOrganismDao
    abstract fun categoryDao(): ICategoryDao
    abstract fun typeCategoryDao(): ITypeCategoryDao
    companion object{
        @Volatile
        private var INSTANCE: MCoreRoomDatabase? = null
        fun getDatabase(context: Context, scope: CoroutineScope): MCoreRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MCoreRoomDatabase::class.java,
                    "MCoreDatabase.db"
                )
                    .setQueryCallback({ sqlQuery, bindArgs ->
                        Log.d("ROOM_SQL", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                    }, Executors.newSingleThreadExecutor())
                    .addCallback(MCoreDatabaseCallback(scope))
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys=ON")
                            Log.d("DB", "Database opened")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
    private class MCoreDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database -> scope.launch { seedDefaults(database) } }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database -> scope.launch { seedDefaults(database) } }
        }

        private suspend fun seedDefaults(database: MCoreRoomDatabase) {
            withContext(Dispatchers.IO) {
                val currencyDao = database.currencyDao()
                if (currencyDao.getAll().isEmpty()) {
                    currencyDao.insertAll(
                        CurrencyModel(
                            id = ExchangeRateRepository.CURRENCY_CDF_ID,
                            name = "Franc Congolais",
                            code = ExchangeRateRepository.CURRENCY_CDF_CODE,
                            symbol = "FC"
                        ),
                        CurrencyModel(
                            id = ExchangeRateRepository.CURRENCY_USD_ID,
                            name = "Dollar Américain",
                            code = ExchangeRateRepository.CURRENCY_USD_CODE,
                            symbol = "$"
                        )
                    )
                }
                val paymentDao = database.paymentMethodDao()
                if (paymentDao.getAll().isEmpty()) {
                    paymentDao.insertAll(
                        PaymentMethodModel(id = 1, name = "Espèces"),
                        PaymentMethodModel(id = 2, name = "Mobile Money"),
                        PaymentMethodModel(id = 3, name = "Virement bancaire")
                    )
                }
                val organismDao = database.organismDao()
                if (organismDao.getAll().isEmpty()) {
                    organismDao.insertAll(OrganismModel(id = 1, name = "MCoreSystem"))
                }
            }
        }
    }
}
