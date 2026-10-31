package elieoko.app.mcoresystem.stock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base locale du module stock, distincte de la base comptable.
 */
@Database(
    entities = [
        StockProduct::class,
        StockLivreur::class,
        MiniStockLine::class,
        StockMovement::class,
        StockOrganism::class,
        StockAccount::class,
        StockOrder::class,
        StockOrderLine::class
    ],
    version = 3,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var instance: StockDatabase? = null

        fun get(context: Context): StockDatabase {
            return instance ?: synchronized(this) {
                instance ?:                 Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "StockLivreurDatabase.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}

/**
 * Conserve les articles déjà saisis et ajoute l'organisation, les comptes,
 * les commandes et le lien mouvement → commande.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `stock_product` ADD COLUMN `organism_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `stock_product` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_livreur` ADD COLUMN `organism_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `stock_livreur` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `mini_stock` ADD COLUMN `organism_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `stock_movement` ADD COLUMN `organism_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `stock_movement` ADD COLUMN `order_id` INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_organism` (`organism_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `name` TEXT NOT NULL, `created_on` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_account` (`account_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `organism_id` INTEGER NOT NULL, `livreur_id` INTEGER, `username` TEXT NOT NULL, `password_hash` TEXT NOT NULL, `role` TEXT NOT NULL, `created_on` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_order` (`order_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `organism_id` INTEGER NOT NULL, `livreur_id` INTEGER, `customer_name` TEXT NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL, `created_on` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_order_line` (`line_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order_id` INTEGER NOT NULL, `product_id` INTEGER NOT NULL, `quantity_ordered` REAL NOT NULL, `quantity_delivered` REAL NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `stock_product` ADD COLUMN `unit_price` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `stock_product` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_livreur` ADD COLUMN `user_uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_livreur` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `mini_stock` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `mini_stock` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_movement` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_movement` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_organism` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_order` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_order` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_order_line` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `stock_order_line` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT ''")
    }
}
