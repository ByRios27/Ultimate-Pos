package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PrizeCalculator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ResumenScreen(
    viewModel: PosViewModel,
    onNavigateToSales: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val draws by viewModel.draws.collectAsState()
    val salesWithItems by viewModel.sales.collectAsState()
    val resultsMap by viewModel.resultsMap.collectAsState()

    val activeSales = remember(salesWithItems) {
        salesWithItems.filter { it.sale.status == "ACTIVA" }
    }

    val allItems = remember(activeSales) {
        activeSales.flatMap { it.items }
    }

    val totalVentas = remember(activeSales) { activeSales.sumOf { it.sale.total } }
    val totalComision = remember(activeSales) { activeSales.sumOf { it.sale.commission } }
    val balanceNeto = totalVentas - totalComision
    val ticketsCount = activeSales.size
    val totalPieces = remember(allItems) { allItems.sumOf { it.quantity } }

    val totalPremios = remember(allItems, resultsMap) {
        allItems.sumOf { item ->
            val result = resultsMap[item.drawId]
            PrizeCalculator.calculateItemPrize(item, result)
        }
    }

    val utilidadReal = balanceNeto - totalPremios

    val salesByDraw = remember(allItems) {
        allItems.groupBy { it.drawName }
            .mapValues { entry -> entry.value.sumOf { it.total } }
            .toList()
            .sortedByDescending { it.second }
    }

    val salesByModality = remember(allItems) {
        allItems.groupBy { it.modality }
            .mapValues { entry ->
                Pair(
                    entry.value.sumOf { it.total },
                    entry.value.sumOf { it.quantity }
                )
            }
    }

    val topNumbers = remember(allItems) {
        allItems.groupBy { it.number }
            .mapValues { entry ->
                Pair(
                    entry.value.sumOf { it.quantity },
                    entry.value.sumOf { it.total }
                )
            }
            .toList()
            .sortedByDescending { it.second.first }
            .take(8)
    }

    val todayDate = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "ES")).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PosGreenAction)
                            )
                            Text(
                                text = "RESUMEN EJECUTIVO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = PosGreenActive,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = todayDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextPrimary
                        )
                        Text(
                            text = "Terminal POS • Turno Activo",
                            fontSize = 11.sp,
                            color = PosTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PosGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = PosBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Primary KPI Cards (2x2 Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Ventas Brutas",
                    value = "$${String.format(Locale.US, "%.2f", totalVentas)}",
                    icon = Icons.Default.AttachMoney,
                    color = PosGreenAction,
                    bgColor = PosPanelSecondary,
                    subtitle = "$ticketsCount tickets ($totalPieces jugadas)",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Comisión (15%)",
                    value = "$${String.format(Locale.US, "%.2f", totalComision)}",
                    icon = Icons.Default.Percent,
                    color = Color(0xFFFF9800),
                    bgColor = PosPanelSecondary,
                    subtitle = "Ganancia por ventas",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Balance Neto",
                    value = "$${String.format(Locale.US, "%.2f", balanceNeto)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = PosInfo,
                    bgColor = PosPanelSecondary,
                    subtitle = "Venta neta acumulada",
                    modifier = Modifier.weight(1f)
                )

                val isDeficit = totalPremios > balanceNeto && totalPremios > 0.0
                StatCard(
                    title = "Utilidad Real",
                    value = "$${String.format(Locale.US, "%.2f", utilidadReal)}",
                    icon = if (isDeficit) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    color = if (isDeficit) PosError else PosGreenAction,
                    bgColor = PosPanelSecondary,
                    subtitle = if (totalPremios > 0) "Premios: $${String.format(Locale.US, "%.2f", totalPremios)}" else "Sin premios pagados",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sales by Draw Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = PosGreenAction,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "VENTAS POR SORTEO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "${salesByDraw.size} sorteo(s)",
                            fontSize = 11.sp,
                            color = PosGreenActive,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (salesByDraw.isEmpty()) {
                        Text(
                            text = "No hay ventas registradas en este turno",
                            fontSize = 13.sp,
                            color = PosTextDisabled,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val maxVal = salesByDraw.firstOrNull()?.second ?: 1.0
                        salesByDraw.forEach { (drawName, amount) ->
                            val progress = if (maxVal > 0) (amount / maxVal).toFloat() else 0f
                            val percentage = if (totalVentas > 0) (amount / totalVentas * 100) else 0.0
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = drawName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PosTextPrimary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", percentage)}%",
                                            fontSize = 11.sp,
                                            color = PosTextSecondary
                                        )
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", amount)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosGreenActive
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = PosGreenAction,
                                    trackColor = PosPanelSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Played Numbers & Modality Breakdown (Side by Side on tablets / grid on mobile)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Played Numbers
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NÚMEROS TOP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        if (topNumbers.isEmpty()) {
                            Text("Sin datos", fontSize = 12.sp, color = PosTextDisabled)
                        } else {
                            topNumbers.forEach { (num, stats) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PosPanelSecondary,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                                    ) {
                                        Text(
                                            text = num,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosGreenActive,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${stats.first}x • $${String.format(Locale.US, "%.0f", stats.second)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Modalidades Breakdown
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "POR MODALIDAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        if (salesByModality.isEmpty()) {
                            Text("Sin datos", fontSize = 12.sp, color = PosTextDisabled)
                        } else {
                            salesByModality.forEach { (mod, stats) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mod,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextPrimary
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", stats.first)}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PosGreenActive
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Operational Metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MÉTRICAS OPERACIONALES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    val ticketPromedio = if (ticketsCount > 0) totalVentas / ticketsCount else 0.0
                    val jugadasPorTicket = if (ticketsCount > 0) totalPieces.toDouble() / ticketsCount else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Ticket Promedio:", fontSize = 13.sp, color = PosTextSecondary)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", ticketPromedio)}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Jugadas por Ticket:", fontSize = 13.sp, color = PosTextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f", jugadasPorTicket),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sorteos Activos en Catálogo:", fontSize = 13.sp, color = PosTextSecondary)
                        Text(
                            text = "${draws.count { it.active }} de ${draws.size}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosGreenActive
                        )
                    }
                }
            }
        }

        // Action Button: Nueva Venta
        item {
            Button(
                onClick = onNavigateToSales,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = PosBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IR A TERMINAL DE VENTAS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PosBackground
                )
            }
        }
    }
}
