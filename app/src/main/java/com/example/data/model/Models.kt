package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val username: String,
    val role: String, // "ADMINISTRADOR", "VENDEDOR", "SUPERVISOR"
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "draws")
data class Draw(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "", // Optional Emoji (e.g. 🇦🇮, 🇩🇴, 🎰)
    val description: String = "",
    val drawDate: String = "", // Specific date e.g. "2026-08-29" or blank for daily
    val drawTime: String = "09:00 AM", // e.g. "09:00 AM"
    val closingTime: String = "08:50 AM", // e.g. "08:50 AM"
    val timezone: String = "America/Managua",
    val allowedModalities: String = "CHANCE,PALE", // Comma-separated: CHANCE, PALE
    val allowedDigits: String = "1,2,3,4", // Comma-separated: 1, 2, 3, 4
    val recurrenceDays: String = "1,2,3,4,5,6,7", // Days of week (1=Mon..7=Sun) or DAILY
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun hasModality(mod: String): Boolean {
        return allowedModalities.split(",").map { it.trim().uppercase() }.contains(mod.uppercase())
    }

    fun hasDigit(d: Int): Boolean {
        return allowedDigits.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(d)
    }

    fun maxDigits(): Int {
        val digits = allowedDigits.split(",").mapNotNull { it.trim().toIntOrNull() }
        return if (digits.isNotEmpty()) digits.maxOrNull() ?: 4 else 4
    }

    fun formattedDisplayName(): String {
        return if (icon.isNotBlank()) "$icon $drawTime — $name" else "$drawTime — $name"
    }
}

@Entity(tableName = "prices")
data class PriceConfig(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val drawId: String, // Specific draw id or "GLOBAL"
    val modality: String, // "CHANCE" or "PALE"
    val digits: Int, // 1, 2, 3, 4
    val unitPrice: Double, // e.g. 0.20
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val alias: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ticketNumber: String, // e.g. "#000125"
    val customerId: String = "",
    val customerName: String, // Cached for historical integrity
    val userId: String,
    val userName: String, // Cached seller name
    val subtotal: Double,
    val commission: Double = 0.0,
    val total: Double,
    val status: String = "ACTIVA", // "ACTIVA", "ANULADA"
    val voidReason: String? = null,
    val voidUserId: String? = null,
    val voidUserName: String? = null,
    val voidDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val drawId: String,
    val drawName: String, // Historical draw name
    val drawIcon: String = "", // Historical icon
    val drawTime: String, // Historical draw time
    val modality: String, // "CHANCE" or "PALE"
    val number: String, // e.g. "25", "78", "25-78"
    val digits: Int,
    val quantity: Double,
    val unitPrice: Double, // Historical unit price at the time of sale
    val total: Double
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val action: String, // "CREAR", "MODIFICAR", "DUPLICAR", "ANULAR", "DESACTIVAR"
    val entity: String, // "SORTEO", "PRECIO", "VENTA", "USUARIO", "SISTEMA"
    val entityId: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "draw_results")
data class DrawResult(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val drawId: String,
    val drawName: String,
    val firstPrize: String = "", // e.g. "94"
    val secondPrize: String = "", // e.g. "04"
    val thirdPrize: String = "", // e.g. "03"
    val drawDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val draw: Draw,
    val modality: String, // "CHANCE" or "PALE"
    val number: String,
    val digits: Int,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double = quantity * unitPrice
)

enum class DrawStatus {
    ACTIVO,
    PROXIMO_A_CERRAR,
    CERRADO,
    INACTIVO
}
