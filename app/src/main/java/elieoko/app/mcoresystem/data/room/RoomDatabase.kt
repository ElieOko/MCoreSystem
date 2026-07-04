package elieoko.app.mcoresystem.data.room

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import elieoko.app.mcoresystem.domain.interfaces.room.*
import elieoko.app.mcoresystem.domain.interfaces.room.IUserDao
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
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MCoreRoomDatabase::class.java,
                    "MCoreDatabase.db"
                )
                    .setQueryCallback({ sqlQuery, bindArgs ->
                        Log.d("ROOM_SQL", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                    }, Executors.newSingleThreadExecutor())
                    //.addMigrations(migrations = (8,9))
                    //.allowMainThreadQueries()
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
            INSTANCE?.let { database->
                scope.launch {}
            }
        }

    }
}
