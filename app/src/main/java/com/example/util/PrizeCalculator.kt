package com.example.util

import com.example.data.local.SaleWithItems
import com.example.data.model.DrawResult
import com.example.data.model.SaleItem
import com.example.data.model.PayoutConfig

object PrizeCalculator {

    /**
     * Calculates prize won by an individual item based on official draw results.
     */
    fun calculateItemPrize(
        item: SaleItem,
        result: DrawResult?,
        config: PayoutConfig = PayoutConfig()
    ): Double {
        if (result == null) return 0.0
        val mod = item.modality.uppercase().trim()
        val num = item.number.trim()
        val p1_full = result.firstPrize.trim()
        val p2_full = result.secondPrize.trim()
        val p3_full = result.thirdPrize.trim()

        val p1 = if (p1_full.length >= 2) p1_full.takeLast(2) else p1_full
        val p2 = if (p2_full.length >= 2) p2_full.takeLast(2) else p2_full
        val p3 = if (p3_full.length >= 2) p3_full.takeLast(2) else p3_full

        return when {
            mod.contains("CHANCE") -> {
                val matchedChance = config.chancePrices.find { Math.abs(it.price - item.unitPrice) < 0.01 } 
                    ?: config.chancePrices.firstOrNull() 
                    ?: com.example.data.model.ChancePriceConfig(0.0, 0.0, 0.0, 0.0)
                
                val mult1 = matchedChance.first
                val mult2 = matchedChance.second
                val mult3 = matchedChance.third
                
                when {
                    p1.isNotBlank() && num == p1 -> item.quantity * mult1
                    p2.isNotBlank() && num == p2 -> item.quantity * mult2
                    p3.isNotBlank() && num == p3 -> item.quantity * mult3
                    else -> 0.0
                }
            }
            mod.contains("PALE") -> {
                val parts = num.split("-").map { it.trim() }
                if (parts.size == 2) {
                    val (n1, n2) = parts
                    when {
                        p1.isNotBlank() && p2.isNotBlank() && ((n1 == p1 && n2 == p2) || (n1 == p2 && n2 == p1)) -> {
                            item.quantity * item.unitPrice * config.paleFirstSecond
                        }
                        p1.isNotBlank() && p3.isNotBlank() && ((n1 == p1 && n2 == p3) || (n1 == p3 && n2 == p1)) -> {
                            item.quantity * item.unitPrice * config.paleFirstThird
                        }
                        p2.isNotBlank() && p3.isNotBlank() && ((n1 == p2 && n2 == p3) || (n1 == p3 && n2 == p2)) -> {
                            item.quantity * item.unitPrice * config.paleSecondThird
                        }
                        else -> 0.0
                    }
                } else {
                    0.0
                }
            }
            mod.contains("BILLETE") -> {
                if (p1_full.isNotBlank() && num == p1_full) {
                    item.quantity * (item.unitPrice * 4000.0)
                } else {
                    0.0
                }
            }
            else -> 0.0
        }
    }

    fun getWinningLabel(
        item: SaleItem,
        result: DrawResult?
    ): String? {
        if (result == null) return null
        val mod = item.modality.uppercase().trim()
        val num = item.number.trim()
        val p1_full = result.firstPrize.trim()
        val p2_full = result.secondPrize.trim()
        val p3_full = result.thirdPrize.trim()

        val p1 = if (p1_full.length >= 2) p1_full.takeLast(2) else p1_full
        val p2 = if (p2_full.length >= 2) p2_full.takeLast(2) else p2_full
        val p3 = if (p3_full.length >= 2) p3_full.takeLast(2) else p3_full

        return when {
            mod.contains("CHANCE") -> {
                when {
                    p1.isNotBlank() && num == p1 -> "1er Premio (70x)"
                    p2.isNotBlank() && num == p2 -> "2do Premio (10x)"
                    p3.isNotBlank() && num == p3 -> "3er Premio (5x)"
                    else -> null
                }
            }
            mod.contains("PALE") -> {
                val parts = num.split("-").map { it.trim() }
                if (parts.size == 2) {
                    val (n1, n2) = parts
                    when {
                        p1.isNotBlank() && p2.isNotBlank() && ((n1 == p1 && n2 == p2) || (n1 == p2 && n2 == p1)) -> "¡Palé 1ro y 2do!"
                        p1.isNotBlank() && p3.isNotBlank() && ((n1 == p1 && n2 == p3) || (n1 == p3 && n2 == p1)) -> "¡Palé 1ro y 3ro!"
                        p2.isNotBlank() && p3.isNotBlank() && ((n1 == p2 && n2 == p3) || (n1 == p3 && n2 == p2)) -> "¡Palé 2do y 3ro!"
                        else -> null
                    }
                } else null
            }
            mod.contains("BILLETE") -> {
                if (p1_full.isNotBlank() && num == p1_full) "¡Billete Mayor!" else null
            }
            else -> null
        }
    }

    /**
     * Calculates total sales for a draw filtering active items.
     */
    fun calculateDrawSales(
        drawId: String,
        sales: List<SaleWithItems>,
        modalityFilter: String? = null
    ): Double {
        return sales
            .filter { it.sale.status == "ACTIVA" }
            .flatMap { it.items }
            .filter { item ->
                item.drawId == drawId &&
                        (modalityFilter == null || item.modality.equals(modalityFilter, ignoreCase = true))
            }
            .sumOf { it.total }
    }

    /**
     * Calculates total prizes won for a draw filtering active items.
     */
    fun calculateDrawPrizes(
        drawId: String,
        sales: List<SaleWithItems>,
        resultsMap: Map<String, DrawResult>,
        modalityFilter: String? = null,
        config: PayoutConfig = PayoutConfig()
    ): Double {
        val result = resultsMap[drawId] ?: return 0.0
        return sales
            .filter { it.sale.status == "ACTIVA" }
            .flatMap { it.items }
            .filter { item ->
                item.drawId == drawId &&
                        (modalityFilter == null || item.modality.equals(modalityFilter, ignoreCase = true))
            }
            .sumOf { calculateItemPrize(it, result, config) }
    }
}
