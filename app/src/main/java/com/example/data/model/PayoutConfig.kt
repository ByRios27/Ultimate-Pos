package com.example.data.model

data class ChancePriceConfig(
    val price: Double,
    val first: Double,
    val second: Double,
    val third: Double
) {
    override fun toString(): String {
        return "$price|$first|$second|$third"
    }

    companion object {
        fun fromString(str: String): ChancePriceConfig? {
            val parts = str.split("|")
            if (parts.size == 4) {
                return ChancePriceConfig(
                    parts[0].toDoubleOrNull() ?: return null,
                    parts[1].toDoubleOrNull() ?: return null,
                    parts[2].toDoubleOrNull() ?: return null,
                    parts[3].toDoubleOrNull() ?: return null
                )
            }
            return null
        }
    }
}

data class PayoutConfig(
    val chancePrices: List<ChancePriceConfig> = listOf(
        ChancePriceConfig(price = 0.20, first = 11.0, second = 3.0, third = 2.0),
        ChancePriceConfig(price = 0.25, first = 14.0, second = 3.0, third = 2.0)
    ),
    val paleFirstSecond: Double = 1000.0,
    val paleSecondThird: Double = 200.0,
    val paleFirstThird: Double = 1000.0,
    val tripletaMultiplier: Double = 20000.0,
    val billeteMultiplier: Double = 4000.0
) {
    fun serializeChances(): String {
        return chancePrices.joinToString(";") { it.toString() }
    }

    companion object {
        fun deserializeChances(str: String): List<ChancePriceConfig> {
            if (str.isBlank()) return listOf(
                ChancePriceConfig(price = 0.20, first = 11.0, second = 3.0, third = 2.0),
                ChancePriceConfig(price = 0.25, first = 14.0, second = 3.0, third = 2.0)
            )
            return str.split(";").mapNotNull { ChancePriceConfig.fromString(it) }
        }
    }
}
