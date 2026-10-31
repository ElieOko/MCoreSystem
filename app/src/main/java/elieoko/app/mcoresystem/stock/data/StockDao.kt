package elieoko.app.mcoresystem.stock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_product ORDER BY name COLLATE NOCASE")
    fun observeProducts(): Flow<List<StockProduct>>

    @Query("SELECT * FROM stock_livreur ORDER BY name COLLATE NOCASE")
    fun observeLivreurs(): Flow<List<StockLivreur>>

    @Query("SELECT * FROM mini_stock")
    fun observeMiniStocks(): Flow<List<MiniStockLine>>

    @Query("SELECT * FROM stock_movement ORDER BY movement_id DESC")
    fun observeMovements(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_product WHERE product_id = :id")
    suspend fun getProduct(id: Long): StockProduct?

    @Query("SELECT * FROM stock_livreur WHERE livreur_id = :id")
    suspend fun getLivreur(id: Long): StockLivreur?

    @Query("SELECT * FROM mini_stock WHERE livreur_id = :livreurId AND product_id = :productId")
    suspend fun getMini(livreurId: Long, productId: Long): MiniStockLine?

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM mini_stock WHERE product_id = :productId")
    suspend fun miniTotalForProduct(productId: Long): Double

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM mini_stock WHERE livreur_id = :livreurId")
    suspend fun miniTotalForLivreur(livreurId: Long): Double

    @Insert
    suspend fun insertProduct(product: StockProduct): Long

    @Update
    suspend fun updateProduct(product: StockProduct)

    @Query("DELETE FROM stock_product WHERE product_id = :id")
    suspend fun deleteProduct(id: Long)

    @Insert
    suspend fun insertLivreur(livreur: StockLivreur): Long

    @Update
    suspend fun updateLivreur(livreur: StockLivreur)

    @Query("DELETE FROM stock_livreur WHERE livreur_id = :id")
    suspend fun deleteLivreur(id: Long)

    @Query("DELETE FROM mini_stock WHERE livreur_id = :livreurId")
    suspend fun deleteMiniForLivreur(livreurId: Long)

    @Insert
    suspend fun insertMini(line: MiniStockLine): Long

    @Update
    suspend fun updateMini(line: MiniStockLine)

    @Query("DELETE FROM mini_stock WHERE mini_stock_id = :id")
    suspend fun deleteMini(id: Long)

    @Insert
    suspend fun insertMovement(movement: StockMovement)
}
