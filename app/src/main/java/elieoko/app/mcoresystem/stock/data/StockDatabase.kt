package elieoko.app.mcoresystem.stock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base locale du module stock, distincte de la base comptable.
 */
@Database(
    entities = [
        StockProduct::class,
        StockLivreur::class,
        MiniStockLine::class,
        StockMovement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var instance: StockDatabase? = null

        fun get(context: Context): StockDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "StockLivreurDatabase.db"
                ).build().also { instance = it }
            }
        }
    }
}
