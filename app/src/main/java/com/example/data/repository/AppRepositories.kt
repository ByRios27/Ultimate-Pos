package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import java.util.UUID

class DrawRepository(private val drawDao: DrawDao, private val auditDao: AuditDao) {
    val allDraws: Flow<List<Draw>> = drawDao.getAllDraws()
    val activeDraws: Flow<List<Draw>> = drawDao.getActiveDraws()

    suspend fun getDrawById(id: String): Draw? = drawDao.getDrawById(id)

    suspend fun insertDraw(draw: Draw, currentUser: User) {
        drawDao.insertDraw(draw)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "CREAR",
                entity = "SORTEO",
                entityId = draw.id,
                oldValue = null,
                newValue = "${draw.icon} ${draw.name} (${draw.drawTime})"
            )
        )
    }

    suspend fun updateDraw(oldDraw: Draw, newDraw: Draw, currentUser: User) {
        drawDao.updateDraw(newDraw)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "MODIFICAR",
                entity = "SORTEO",
                entityId = newDraw.id,
                oldValue = "${oldDraw.name} - ${oldDraw.drawTime} - Cierre: ${oldDraw.closingTime}",
                newValue = "${newDraw.name} - ${newDraw.drawTime} - Cierre: ${newDraw.closingTime}"
            )
        )
    }

    suspend fun duplicateDraw(sourceDraw: Draw, newName: String, newTime: String, newClosingTime: String, currentUser: User): Draw {
        val duplicated = sourceDraw.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            drawTime = newTime,
            closingTime = newClosingTime,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        drawDao.insertDraw(duplicated)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "DUPLICAR",
                entity = "SORTEO",
                entityId = duplicated.id,
                oldValue = "Origen: ${sourceDraw.name} (${sourceDraw.drawTime})",
                newValue = "Copia: ${duplicated.name} (${duplicated.drawTime})"
            )
        )
        return duplicated
    }

    suspend fun deleteDraw(draw: Draw, currentUser: User) {
        drawDao.deleteDraw(draw)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "ELIMINAR",
                entity = "SORTEO",
                entityId = draw.id,
                oldValue = "${draw.name} (${draw.drawTime})",
                newValue = null
            )
        )
    }

    suspend fun toggleActive(draw: Draw, currentUser: User) {
        val updated = draw.copy(active = !draw.active, updatedAt = System.currentTimeMillis())
        drawDao.updateDraw(updated)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = if (updated.active) "ACTIVAR" else "DESACTIVAR",
                entity = "SORTEO",
                entityId = draw.id,
                oldValue = "Activo: ${draw.active}",
                newValue = "Activo: ${updated.active}"
            )
        )
    }
}

class PriceRepository(private val priceDao: PriceDao, private val auditDao: AuditDao) {
    val allPrices: Flow<List<PriceConfig>> = priceDao.getAllPrices()

    fun getPricesForDraw(drawId: String): Flow<List<PriceConfig>> = priceDao.getPricesForDraw(drawId)

    suspend fun getUnitPrice(drawId: String, modality: String, digits: Int): Double {
        val price = priceDao.getPrice(drawId, modality, digits)
        return price?.unitPrice ?: 0.20
    }

    suspend fun saveOrUpdatePrice(price: PriceConfig, currentUser: User) {
        priceDao.insertPrice(price)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "MODIFICAR",
                entity = "PRECIO",
                entityId = price.id,
                oldValue = null,
                newValue = "${price.modality} ${price.digits} cifras -> $${String.format("%.2f", price.unitPrice)} (Draw: ${price.drawId})"
            )
        )
    }

    suspend fun deletePricesForDraw(drawId: String) {
        priceDao.deletePricesForDraw(drawId)
    }
}

class CustomerRepository(private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)

    suspend fun insertCustomer(customer: Customer) = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    suspend fun getCustomerById(id: String): Customer? = customerDao.getCustomerById(id)
}

class UserRepository(private val userDao: UserDao, private val auditDao: AuditDao) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val activeUsers: Flow<List<User>> = userDao.getActiveUsers()

    suspend fun getUserById(id: String): User? = userDao.getUserById(id)

    suspend fun insertUser(user: User, currentUser: User) {
        userDao.insertUser(user)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "CREAR",
                entity = "USUARIO",
                entityId = user.id,
                oldValue = null,
                newValue = "${user.name} (${user.role})"
            )
        )
    }

    suspend fun updateUser(user: User, currentUser: User) {
        userDao.updateUser(user)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "MODIFICAR",
                entity = "USUARIO",
                entityId = user.id,
                oldValue = null,
                newValue = "${user.name} (${user.role}) - Activo: ${user.active}"
            )
        )
    }
}

class SalesRepository(
    private val saleDao: SaleDao,
    private val auditDao: AuditDao,
    private val customerDao: CustomerDao
) {
    val allSalesWithItems: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()
    val allSaleItems: Flow<List<SaleItem>> = saleDao.getAllSaleItems()

    suspend fun createSale(
        customerName: String,
        currentUser: User,
        cartItems: List<CartItem>,
        commissionRate: Double = 0.05
    ): SaleWithItems {
        // Find or create customer if needed
        var customerId = ""
        val trimmedCustName = customerName.trim()
        val customer = Customer(
            name = trimmedCustName,
            alias = trimmedCustName
        )
        customerDao.insertCustomer(customer)
        customerId = customer.id

        val count = saleDao.getTotalSalesCount()
        val ticketNumber = String.format("#%06d", count + 126)

        val subtotal = cartItems.sumOf { it.total }
        val commission = subtotal * commissionRate
        val total = subtotal

        val sale = Sale(
            id = UUID.randomUUID().toString(),
            ticketNumber = ticketNumber,
            customerId = customerId,
            customerName = trimmedCustName,
            userId = currentUser.id,
            userName = currentUser.name,
            subtotal = subtotal,
            commission = commission,
            total = total,
            status = "ACTIVA",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val saleItems = cartItems.map { item ->
            SaleItem(
                id = UUID.randomUUID().toString(),
                saleId = sale.id,
                drawId = item.draw.id,
                drawName = item.draw.name,
                drawIcon = item.draw.icon,
                drawTime = item.draw.drawTime,
                modality = item.modality,
                number = item.number,
                digits = item.digits,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                total = item.total
            )
        }

        saleDao.insertSale(sale)
        saleDao.insertSaleItems(saleItems)

        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "CREAR",
                entity = "VENTA",
                entityId = sale.id,
                oldValue = null,
                newValue = "Venta $ticketNumber - Cliente: $trimmedCustName - Total: $${String.format("%.2f", total)} (${saleItems.size} jugadas)"
            )
        )

        return SaleWithItems(sale, saleItems)
    }

    suspend fun voidSale(saleId: String, reason: String, currentUser: User) {
        val saleWithItems = saleDao.getSaleWithItemsById(saleId)
        saleDao.voidSale(
            saleId = saleId,
            reason = reason,
            userId = currentUser.id,
            userName = currentUser.name,
            voidDate = System.currentTimeMillis()
        )
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "ANULAR",
                entity = "VENTA",
                entityId = saleId,
                oldValue = "Venta activa ${saleWithItems?.sale?.ticketNumber ?: ""}",
                newValue = "ANULADA. Motivo: $reason"
            )
        )
    }

    suspend fun updateSaleCustomerName(saleId: String, newCustomerName: String, currentUser: User) {
        val trimmed = newCustomerName.trim()
        val oldSale = saleDao.getSaleWithItemsById(saleId)
        val oldName = oldSale?.sale?.customerName ?: ""
        saleDao.updateCustomerName(saleId, trimmed)
        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "MODIFICAR",
                entity = "VENTA",
                entityId = saleId,
                oldValue = "Cliente: $oldName",
                newValue = "Cliente: $trimmed"
            )
        )
    }

    suspend fun updateSaleItem(item: SaleItem, currentUser: User) {
        saleDao.updateSaleItem(item)
        val sale = saleDao.getSaleWithItemsById(item.saleId)
        if (sale != null) {
            val newSubtotal = sale.items.sumOf { it.total }
            val newCommission = newSubtotal * (sale.sale.commission / if (sale.sale.subtotal > 0) sale.sale.subtotal else 1.0)
            saleDao.updateSale(
                sale.sale.copy(
                    subtotal = newSubtotal,
                    commission = newCommission,
                    total = newSubtotal,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateSaleWithItems(
        saleId: String,
        customerName: String,
        currentUser: User,
        cartItems: List<CartItem>,
        commissionRate: Double = 0.05
    ): SaleWithItems {
        val original = saleDao.getSaleWithItemsById(saleId)
        val subtotal = cartItems.sumOf { it.total }
        val commission = subtotal * commissionRate
        val total = subtotal
        val trimmedName = customerName.trim().ifBlank { original?.sale?.customerName ?: "Cliente Mostrador" }

        // Ensure customer exists
        val customer = Customer(name = trimmedName, alias = trimmedName)
        customerDao.insertCustomer(customer)

        val updatedSale = (original?.sale ?: Sale(
            id = saleId,
            ticketNumber = "#000000",
            customerId = customer.id,
            customerName = trimmedName,
            userId = currentUser.id,
            userName = currentUser.name,
            subtotal = subtotal,
            commission = commission,
            total = total,
            status = "ACTIVA",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )).copy(
            customerName = trimmedName,
            subtotal = subtotal,
            commission = commission,
            total = total,
            updatedAt = System.currentTimeMillis()
        )

        saleDao.updateSale(updatedSale)
        saleDao.deleteSaleItems(saleId)

        val newItems = cartItems.map { item ->
            SaleItem(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                drawId = item.draw.id,
                drawName = item.draw.name,
                drawIcon = item.draw.icon,
                drawTime = item.draw.drawTime,
                modality = item.modality,
                number = item.number,
                digits = item.digits,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                total = item.total
            )
        }
        saleDao.insertSaleItems(newItems)

        auditDao.insertLog(
            AuditLog(
                userId = currentUser.id,
                userName = currentUser.name,
                action = "MODIFICAR",
                entity = "VENTA",
                entityId = saleId,
                oldValue = "Venta ${original?.sale?.ticketNumber ?: saleId} - Total anterior: $${String.format(Locale.US, "%.2f", original?.sale?.total ?: 0.0)}",
                newValue = "Venta Editada ${updatedSale.ticketNumber} - Cliente: $trimmedName - Nuevo Total: $${String.format(Locale.US, "%.2f", total)} (${newItems.size} jugadas)"
            )
        )

        return SaleWithItems(updatedSale, newItems)
    }

    suspend fun getSaleWithItemsById(saleId: String): SaleWithItems? {
        return saleDao.getSaleWithItemsById(saleId)
    }
}

class AuditRepository(private val auditDao: AuditDao) {
    val allLogs: Flow<List<AuditLog>> = auditDao.getAllLogs()
}

class DrawResultRepository(private val resultDao: DrawResultDao) {
    val allResults: Flow<List<DrawResult>> = resultDao.getAllResults()

    suspend fun getResultForDraw(drawId: String): DrawResult? = resultDao.getResultForDraw(drawId)
    suspend fun getResultByDrawName(drawName: String): DrawResult? = resultDao.getResultByDrawName(drawName)
    suspend fun insertResult(result: DrawResult) = resultDao.insertResult(result)
}

