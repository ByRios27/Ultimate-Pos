package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Query("SELECT * FROM draws ORDER BY active DESC, drawTime ASC")
    fun getAllDraws(): Flow<List<Draw>>

    @Query("SELECT * FROM draws WHERE active = 1 ORDER BY drawTime ASC")
    fun getActiveDraws(): Flow<List<Draw>>

    @Query("SELECT * FROM draws WHERE id = :id")
    suspend fun getDrawById(id: String): Draw?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraw(draw: Draw)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraws(draws: List<Draw>)

    @Update
    suspend fun updateDraw(draw: Draw)

    @Delete
    suspend fun deleteDraw(draw: Draw)

    @Query("SELECT COUNT(*) FROM draws")
    suspend fun getDrawCount(): Int

    @Query("DELETE FROM draws WHERE id = :id")
    suspend fun deleteDrawById(id: String)
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM prices WHERE active = 1")
    fun getAllPrices(): Flow<List<PriceConfig>>

    @Query("SELECT * FROM prices WHERE drawId = :drawId AND active = 1")
    fun getPricesForDraw(drawId: String): Flow<List<PriceConfig>>

    @Query("SELECT * FROM prices WHERE (drawId = :drawId OR drawId = 'GLOBAL') AND modality = :modality AND digits = :digits AND active = 1 ORDER BY CASE WHEN drawId = 'GLOBAL' THEN 1 ELSE 0 END ASC LIMIT 1")
    suspend fun getPrice(drawId: String, modality: String, digits: Int): PriceConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: PriceConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<PriceConfig>)

    @Update
    suspend fun updatePrice(price: PriceConfig)

    @Delete
    suspend fun deletePrice(price: PriceConfig)

    @Query("DELETE FROM prices WHERE drawId = :drawId")
    suspend fun deletePricesForDraw(drawId: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE active = 1")
    fun getActiveUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR alias LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)
}

data class SaleWithItems(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItem>
)

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleWithItemsById(saleId: String): SaleWithItems?

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE status = 'ACTIVA' ORDER BY createdAt DESC")
    fun getActiveSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sale_items ORDER BY id DESC")
    fun getAllSaleItems(): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: String): List<SaleItem>

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteSaleItems(saleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Update
    suspend fun updateSaleItem(item: SaleItem)

    @Query("UPDATE sales SET customerName = :newCustomerName, updatedAt = :updatedAt WHERE id = :saleId")
    suspend fun updateCustomerName(saleId: String, newCustomerName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sales SET status = 'ANULADA', voidReason = :reason, voidUserId = :userId, voidUserName = :userName, voidDate = :voidDate WHERE id = :saleId")
    suspend fun voidSale(saleId: String, reason: String, userId: String, userName: String, voidDate: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getTotalSalesCount(): Int
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)
}

@Dao
interface DrawResultDao {
    @Query("SELECT * FROM draw_results ORDER BY createdAt DESC")
    fun getAllResults(): Flow<List<DrawResult>>

    @Query("SELECT * FROM draw_results WHERE drawId = :drawId LIMIT 1")
    suspend fun getResultForDraw(drawId: String): DrawResult?

    @Query("SELECT * FROM draw_results WHERE drawName LIKE '%' || :drawName || '%' LIMIT 1")
    suspend fun getResultByDrawName(drawName: String): DrawResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: DrawResult)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<DrawResult>)
}

