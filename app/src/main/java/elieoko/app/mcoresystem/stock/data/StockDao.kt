package elieoko.app.mcoresystem.stock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_product WHERE organism_id = :organismId ORDER BY name COLLATE NOCASE")
    fun observeProducts(organismId: Long): Flow<List<StockProduct>>

    @Query("SELECT * FROM stock_livreur WHERE organism_id = :organismId ORDER BY name COLLATE NOCASE")
    fun observeLivreurs(organismId: Long): Flow<List<StockLivreur>>

    @Query("SELECT * FROM mini_stock WHERE organism_id = :organismId")
    fun observeMiniStocks(organismId: Long): Flow<List<MiniStockLine>>

    @Query("SELECT * FROM stock_movement WHERE organism_id = :organismId ORDER BY movement_id DESC")
    fun observeMovements(organismId: Long): Flow<List<StockMovement>>

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

    @Update
    suspend fun updateMovement(movement: StockMovement)

    @Query("SELECT COUNT(*) FROM stock_account")
    fun observeAccountCount(): Flow<Int>

    @Query("SELECT * FROM stock_account")
    suspend fun allAccounts(): List<StockAccount>

    @Query("SELECT * FROM stock_account WHERE livreur_id = :livreurId LIMIT 1")
    suspend fun accountForLivreur(livreurId: Long): StockAccount?

    @Query("SELECT COUNT(*) FROM stock_organism")
    suspend fun countOrganisms(): Int

    @Query("SELECT * FROM stock_organism WHERE organism_id = :id")
    suspend fun getOrganism(id: Long): StockOrganism?

    @Query("SELECT * FROM stock_organism")
    suspend fun allOrganisms(): List<StockOrganism>

    @Query("SELECT * FROM stock_organism WHERE uuid = :uuid LIMIT 1")
    suspend fun organismByUuid(uuid: String): StockOrganism?

    @Update
    suspend fun updateOrganism(organism: StockOrganism)

    @Query("SELECT * FROM stock_product WHERE organism_id = :organismId")
    suspend fun productsOf(organismId: Long): List<StockProduct>

    @Query("SELECT * FROM stock_livreur WHERE organism_id = :organismId")
    suspend fun livreursOf(organismId: Long): List<StockLivreur>

    @Query("SELECT * FROM stock_livreur WHERE user_uuid = :userUuid AND organism_id = :organismId LIMIT 1")
    suspend fun livreurByUserUuid(userUuid: String, organismId: Long): StockLivreur?

    @Query("SELECT * FROM mini_stock WHERE organism_id = :organismId")
    suspend fun minisOf(organismId: Long): List<MiniStockLine>

    @Query("SELECT * FROM stock_movement WHERE organism_id = :organismId")
    suspend fun movementsOf(organismId: Long): List<StockMovement>

    @Query("SELECT * FROM stock_order WHERE organism_id = :organismId")
    suspend fun ordersOf(organismId: Long): List<StockOrder>

    @Query("SELECT * FROM stock_order_line WHERE order_id = :orderId")
    suspend fun linesOf(orderId: Long): List<StockOrderLine>

    @Query("SELECT * FROM stock_product WHERE uuid = :uuid LIMIT 1")
    suspend fun productByUuid(uuid: String): StockProduct?

    @Query("SELECT * FROM stock_livreur WHERE uuid = :uuid LIMIT 1")
    suspend fun livreurByUuid(uuid: String): StockLivreur?

    @Query("SELECT * FROM mini_stock WHERE uuid = :uuid LIMIT 1")
    suspend fun miniByUuid(uuid: String): MiniStockLine?

    @Query("SELECT * FROM stock_movement WHERE uuid = :uuid LIMIT 1")
    suspend fun movementByUuid(uuid: String): StockMovement?

    @Query("SELECT * FROM stock_order WHERE uuid = :uuid LIMIT 1")
    suspend fun orderByUuid(uuid: String): StockOrder?

    @Query("SELECT * FROM stock_order_line WHERE uuid = :uuid LIMIT 1")
    suspend fun orderLineByUuid(uuid: String): StockOrderLine?

    @Insert
    suspend fun insertOrganism(organism: StockOrganism): Long

    @Insert
    suspend fun insertAccount(account: StockAccount): Long

    @Update
    suspend fun updateAccount(account: StockAccount)

    @Query("DELETE FROM stock_account WHERE livreur_id = :livreurId")
    suspend fun deleteAccountForLivreur(livreurId: Long)

    @Query("SELECT * FROM stock_account WHERE organism_id = :organismId")
    fun observeAccounts(organismId: Long): Flow<List<StockAccount>>

    @Query("UPDATE stock_product SET organism_id = :organismId WHERE organism_id = 0")
    suspend fun claimProducts(organismId: Long)

    @Query("UPDATE stock_livreur SET organism_id = :organismId WHERE organism_id = 0")
    suspend fun claimLivreurs(organismId: Long)

    @Query("UPDATE mini_stock SET organism_id = :organismId WHERE organism_id = 0")
    suspend fun claimMiniStocks(organismId: Long)

    @Query("UPDATE stock_movement SET organism_id = :organismId WHERE organism_id = 0")
    suspend fun claimMovements(organismId: Long)

    @Query("UPDATE stock_order SET organism_id = :organismId WHERE organism_id = 0")
    suspend fun claimOrders(organismId: Long)

    @Query("SELECT COUNT(*) FROM stock_account WHERE organism_id = :organismId AND lower(username) = lower(:username)")
    suspend fun usernameTaken(organismId: Long, username: String): Int

    @Query("SELECT * FROM stock_order WHERE organism_id = :organismId ORDER BY order_id DESC")
    fun observeOrders(organismId: Long): Flow<List<StockOrder>>

    @Query("SELECT * FROM stock_order_line WHERE order_id IN (SELECT order_id FROM stock_order WHERE organism_id = :organismId)")
    fun observeOrderLines(organismId: Long): Flow<List<StockOrderLine>>

    @Query("SELECT * FROM stock_order WHERE order_id = :id")
    suspend fun getOrder(id: Long): StockOrder?

    @Query("SELECT * FROM stock_order_line WHERE order_id = :orderId")
    suspend fun linesForOrder(orderId: Long): List<StockOrderLine>

    @Query("SELECT * FROM stock_order_line WHERE order_id = :orderId AND product_id = :productId LIMIT 1")
    suspend fun getOrderLine(orderId: Long, productId: Long): StockOrderLine?

    @Insert
    suspend fun insertOrder(order: StockOrder): Long

    @Update
    suspend fun updateOrder(order: StockOrder)

    @Insert
    suspend fun insertOrderLine(line: StockOrderLine): Long

    @Update
    suspend fun updateOrderLine(line: StockOrderLine)
}
