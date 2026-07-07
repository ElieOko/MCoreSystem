package elieoko.app.mcoresystem.data.room

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import elieoko.app.mcoresystem.domain.interfaces.room.*
import elieoko.app.mcoresystem.domain.model.room.*
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@Database(
    entities = [CurrencyModel::class, PaymentMethodModel::class, OperationModel::class, CategoryModel::class, TypeCategoryModel::class, OrganismModel::class, UserModel::class, SyncQueueModel::class], version = 3, exportSchema = false)
abstract class MCoreRoomDatabase : RoomDatabase() {
    abstract fun currencyDao(): ICurrencyDao
    abstract fun paymentMethodDao(): IPaymentMethodDao
    abstract fun userDao(): IUserDao
    abstract fun operationDao(): IOperationDao
    abstract fun organismDao(): IOrganismDao
    abstract fun categoryDao(): ICategoryDao
    abstract fun typeCategoryDao(): ITypeCategoryDao
    abstract fun syncQueueDao(): ISyncQueueDao
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
                    .fallbackToDestructiveMigration(true)
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
    // Aucune donnée n'est créée par défaut : l'utilisateur crée son organisation
    // à l'inscription, puis ses devises, types, catégories et modes de paiement.
}
