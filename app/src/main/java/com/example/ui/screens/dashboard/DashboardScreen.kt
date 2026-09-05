package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SaleWithItems
import com.example.data.model.Draw
import com.example.data.model.DrawResult
import com.example.data.model.SaleItem
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PrizeCalculator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
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

    // Collect all items across active sales
    val allItems = remember(activeSales) {
        activeSales.flatMap { it.items }
    }

    // Group items by drawId
    val itemsByDrawId = remember(allItems) {
        allItems.groupBy { it.drawId }
    }

    // Filter draws to those that have sales, or all active draws
    val drawsWithSales = remember(draws, itemsByDrawId) {
        val drawsWithActualSales = draws.filter { (itemsByDrawId[it.id]?.sumOf { item -> item.quantity } ?: 0.0) > 0.0 }
        if (drawsWithActualSales.isNotEmpty()) {
            drawsWithActualSales
        } else {
            draws.take(6)
        }
    }

    // Track which draw accordion is expanded (default to all closed upon entering reports/dashboard)
    var expandedDrawId by remember { mutableStateOf("") }

    // Track selected number or combination to show "Detalle de Ventas" floating modal
    var selectedItemKey by remember { mutableStateOf<String?>(null) } // e.g. "CHANCE:30" or "PALE:03-85"

    // Track selected sale to show receipt dialog
    var selectedSaleForReceipt by remember { mutableStateOf<SaleWithItems?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PosGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = PosBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "REPORTE POR SORTEO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = PosGreenActive,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Matriz de números, piezas y combinaciones",
                                fontSize = 10.5.sp,
                                color = PosTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PosPanelSecondary,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                    ) {
                        Text(
                            text = "${drawsWithSales.size} sorteo(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosGreenActive,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // List of Draw Accordions
        items(drawsWithSales, key = { it.id }) { draw ->
            val isExpanded = expandedDrawId == draw.id
            val itemsForDraw = itemsByDrawId[draw.id] ?: emptyList()
            val totalPieces = itemsForDraw.sumOf { it.quantity }
            val drawResult = resultsMap[draw.id]

            DrawStatsAccordionCard(
                draw = draw,
                itemsForDraw = itemsForDraw,
                totalPieces = totalPieces,
                drawResult = drawResult,
                isExpanded = isExpanded,
                selectedItemKey = if (isExpanded) selectedItemKey else null,
                allSales = activeSales,
                onToggleExpand = {
                    if (isExpanded) {
                        expandedDrawId = ""
                        selectedItemKey = null
                    } else {
                        expandedDrawId = draw.id
                        selectedItemKey = null
                    }
                },
                onSelectKey = { key ->
                    selectedItemKey = if (selectedItemKey == key) null else key
                },
                onClearSelection = {
                    selectedItemKey = null
                },
                onShowTicket = { saleWithItems ->
                    selectedSaleForReceipt = saleWithItems
                }
            )
        }
    }

    // Modal dialog to view the full thermal receipt for the clicked ticket
    selectedSaleForReceipt?.let { saleWithItems ->
        ReceiptDialog(
            saleWithItems = saleWithItems,
            resultsMap = resultsMap,
            onDismiss = { selectedSaleForReceipt = null },
            onNewSale = { selectedSaleForReceipt = null }
        )
    }
}

// ----------------------------------------------------------------------
// ACCORDION DRAW CARD COMPONENT
// ----------------------------------------------------------------------
@Composable
private fun DrawStatsAccordionCard(
    draw: Draw,
    itemsForDraw: List<SaleItem>,
    totalPieces: Double,
    drawResult: DrawResult?,
    isExpanded: Boolean,
    selectedItemKey: String?,
    allSales: List<SaleWithItems>,
    onToggleExpand: () -> Unit,
    onSelectKey: (String) -> Unit,
    onClearSelection: () -> Unit,
    onShowTicket: (SaleWithItems) -> Unit
) {
    val drawTotalSales = remember(itemsForDraw) { itemsForDraw.sumOf { it.total } }
    val drawTotalCommission = remember(drawTotalSales) { drawTotalSales * 0.15 }
    val drawTotalPrizes = remember(itemsForDraw, drawResult) {
        itemsForDraw.sumOf { item -> PrizeCalculator.calculateItemPrize(item, drawResult) }
    }
    // RED RULE: El color rojo solo se aplica cuando el total de premios supere la utilidad de ese sorteo
    val isDeficit = drawResult != null && drawTotalPrizes > (drawTotalSales - drawTotalCommission) && drawTotalPrizes > 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDeficit) Color(0xFF200B0F) else PosPanel),
        border = androidx.compose.foundation.BorderStroke(
            if (isDeficit) 1.5.dp else 1.dp,
            when {
                isDeficit -> Color(0xFFEF4444)
                isExpanded -> PosGreenAction.copy(alpha = 0.6f)
                else -> PosBorder
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Accordion Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Expand/Collapse Circle Button + Draw details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Circle toggle icon (+ or X)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDeficit -> Color(0xFF7F1D1D)
                                    isExpanded -> Color(0xFF064E3B)
                                    else -> PosPanelSecondary
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isDeficit -> Color(0xFFEF4444)
                                    isExpanded -> PosGreenAction
                                    else -> PosBorder
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                            tint = when {
                                isDeficit -> Color(0xFFFCA5A5)
                                isExpanded -> PosGreenActive
                                else -> PosGreenAction
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${draw.icon} ${draw.drawTime} ${draw.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PosTextPrimary
                            )
                            if (draw.drawTime.contains("PM", ignoreCase = true) || draw.drawTime.contains("12:", ignoreCase = true)) {
                                Text("☀️", fontSize = 12.sp)
                            } else {
                                Text("🌙", fontSize = 12.sp)
                            }
                            if (drawResult != null) {
                                Text("🏆", fontSize = 12.sp)
                            }
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Cerrado",
                                tint = PosTextDisabled,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val todayStr = remember {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            }
                            Text(
                                text = draw.drawDate.ifBlank { todayStr },
                                fontSize = 10.5.sp,
                                color = PosTextSecondary
                            )

                            // If collapsed and has results, show compact pills
                            if (!isExpanded && drawResult != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    CompactPrizeBadge(drawResult.firstPrize, Color(0xFFF59E0B), Color(0xFFFEF3C7))
                                    CompactPrizeBadge(drawResult.secondPrize, Color(0xFF94A3B8), Color(0xFFF1F5F9))
                                    CompactPrizeBadge(drawResult.thirdPrize, Color(0xFFF97316), Color(0xFFFFEDD5))
                                }
                            }
                        }
                    }
                }

                // Right: Pieces Count Badge (PZS 154)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PZS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary
                    )
                    Text(
                        text = "$totalPieces",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDeficit) Color(0xFFEF4444) else PosTextPrimary
                    )
                }
            }

            // Accordion Content (Visible when Expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = PosBorder)

                    // Results Badges (1RO, 2DO, 3RO)
                    if (drawResult != null) {
                        DrawResultsBanner(drawResult = drawResult)
                    }

                    // Detalle de Ventas Card (Appears at TOP of Grid when a number/pale/billete is clicked)
                    if (selectedItemKey != null) {
                        SalesDetailTopPopup(
                            selectedKey = selectedItemKey,
                            draw = draw,
                            itemsForDraw = itemsForDraw,
                            allSales = allSales,
                            onClose = onClearSelection,
                            onShowTicket = onShowTicket
                        )
                    }

                    // 10x10 Heatmap Matrix (00 - 99)
                    DrawHeatmapGrid(
                        itemsForDraw = itemsForDraw,
                        drawResult = drawResult,
                        selectedKey = selectedItemKey,
                        onNumberClick = { numStr ->
                            onSelectKey("CHANCE:$numStr")
                        }
                    )

                    // Volume Bar Legend
                    VolumeGradientLegend()

                    // Combinaciones Vendidas (Pales/Pares)
                    CombinacionesSection(
                        itemsForDraw = itemsForDraw,
                        selectedKey = selectedItemKey,
                        onComboClick = { comboStr ->
                            onSelectKey("PALE:$comboStr")
                        }
                    )

                    // Billetes Vendidos (3-4 Dígitos)
                    BilletesSection(
                        itemsForDraw = itemsForDraw,
                        selectedKey = selectedItemKey,
                        onBilleteClick = { billeteStr ->
                            onSelectKey("BILLETE:$billeteStr")
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// COMPACT PRIZE BADGE (Collapsed header)
// ----------------------------------------------------------------------
@Composable
private fun CompactPrizeBadge(
    number: String,
    badgeColor: Color,
    textColor: Color
) {
    if (number.isNotBlank()) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor,
            modifier = Modifier.height(18.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// RESULTS BANNER (1RO, 2DO, 3RO in expanded draw)
// ----------------------------------------------------------------------
@Composable
private fun DrawResultsBanner(drawResult: DrawResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1RO Prize (Gold)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text("1RO", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFBBF24), // Vibrant Yellow Gold
                modifier = Modifier
                    .width(48.dp)
                    .height(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = drawResult.firstPrize.ifBlank { "--" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E1B4B)
                    )
                }
            }
        }

        // 2DO Prize (Silver / Slate)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text("2DO", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFCBD5E1), // Silver / White Slate
                modifier = Modifier
                    .width(48.dp)
                    .height(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = drawResult.secondPrize.ifBlank { "--" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }

        // 3RO Prize (Bronze / Orange)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text("3RO", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFB923C), // Bronze Orange
                modifier = Modifier
                    .width(48.dp)
                    .height(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = drawResult.thirdPrize.ifBlank { "--" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF431407)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// SALES DETAIL TOP POPUP (Appears at TOP of Grid when item clicked)
// ----------------------------------------------------------------------
@Composable
private fun SalesDetailTopPopup(
    selectedKey: String,
    draw: Draw,
    itemsForDraw: List<SaleItem>,
    allSales: List<SaleWithItems>,
    onClose: () -> Unit,
    onShowTicket: (SaleWithItems) -> Unit
) {
    val parts = selectedKey.split(":")
    val modalityType = parts.getOrNull(0) ?: "CHANCE"
    val targetNumber = parts.getOrNull(1) ?: ""

    // Find all sales tickets containing this number for this draw
    val matchingSales = remember(selectedKey, itemsForDraw, allSales) {
        val result = mutableListOf<Pair<SaleWithItems, SaleItem>>()
        allSales.forEach { saleWithItem ->
            saleWithItem.items.forEach { itm ->
                if (itm.drawId == draw.id) {
                    val isMatch = when (modalityType) {
                        "CHANCE" -> itm.number.padStart(2, '0') == targetNumber.padStart(2, '0')
                        "PALE" -> itm.number == targetNumber || itm.number.replace(" ", "") == targetNumber.replace(" ", "")
                        "BILLETE" -> itm.number == targetNumber
                        else -> itm.number == targetNumber
                    }
                    if (isMatch) {
                        result.add(Pair(saleWithItem, itm))
                    }
                }
            }
        }
        result
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B131B)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PosGreenAction)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Big Number Badge + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big Rounded Number Badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PosGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = targetNumber,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = PosBackground
                        )
                    }

                    Column {
                        Text(
                            text = "Detalle de Ventas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = when (modalityType) {
                                "CHANCE" -> "CLIENTES QUE COMPRARON EL $targetNumber"
                                "PALE" -> "CLIENTES CON COMBINACIÓN $targetNumber"
                                else -> "CLIENTES CON BILLETE $targetNumber"
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosGreenActive,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // Close Button (X)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = PosTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Divider(color = PosBorder)

            // Ticket List
            if (matchingSales.isEmpty()) {
                Text(
                    text = "No se encontraron tickets registrados para este número.",
                    fontSize = 11.sp,
                    color = PosTextDisabled,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchingSales.forEach { (saleWithItem, item) ->
                        val sale = saleWithItem.sale
                        val timeStr = remember(sale.createdAt) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(sale.createdAt)).lowercase()
                        }
                        val dateStr = remember(sale.createdAt) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(sale.createdAt))
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onShowTicket(saleWithItem) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Top row: Avatar + Customer Name/Phone + Amount & Multiplier
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E293B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = PosTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = sale.customerName.ifBlank { "Cliente ${sale.ticketNumber}" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosTextPrimary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", item.total)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = PosTextPrimary
                                        )
                                        Text(
                                            text = "x${item.quantity}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PosGreenActive
                                        )
                                    }
                                }

                                // Sorteo info and time
                                Text(
                                    text = "$timeStr  •  ${draw.icon} ${draw.drawTime} ${draw.name} ☀️  •  $dateStr",
                                    fontSize = 10.sp,
                                    color = PosTextSecondary,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Bottom right ticket icon button
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    IconButton(
                                        onClick = { onShowTicket(saleWithItem) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Receipt,
                                            contentDescription = "Ver Ticket ${sale.ticketNumber}",
                                            tint = PosTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 10x10 HEATMAP GRID COMPONENT (00 - 99)
// ----------------------------------------------------------------------
@Composable
private fun DrawHeatmapGrid(
    itemsForDraw: List<SaleItem>,
    drawResult: DrawResult?,
    selectedKey: String?,
    onNumberClick: (String) -> Unit
) {
    // Count quantities by 2-digit number (00..99)
    val numberCounts = remember(itemsForDraw) {
        val map = mutableMapOf<String, Double>()
        itemsForDraw.filter { it.number.length <= 2 && it.modality == "CHANCE" }.forEach { item ->
            val formatted = item.number.padStart(2, '0')
            map[formatted] = (map[formatted] ?: 0.0) + item.quantity
        }
        map
    }

    val maxQty = remember(numberCounts) {
        numberCounts.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    }

    val selectedNumber = if (selectedKey?.startsWith("CHANCE:") == true) {
        selectedKey.substringAfter("CHANCE:")
    } else null

    // 10 Columns x 10 Rows
    // Column 0: 00..09, Column 1: 10..19, ... Column 9: 90..99
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (row in 0..9) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                for (col in 0..9) {
                    val numInt = col * 10 + row
                    val numStr = String.format(Locale.US, "%02d", numInt)
                    val qty = numberCounts[numStr] ?: 0.0

                    val isFirstPrize = drawResult?.firstPrize == numStr
                    val isSecondPrize = drawResult?.secondPrize == numStr
                    val isThirdPrize = drawResult?.thirdPrize == numStr
                    val isSelected = selectedNumber == numStr

                    // Determine Cell Background Color based on volume
                    val cellBgColor = when {
                        qty == 0.0 -> Color(0xFF0C101A)
                        else -> {
                            val ratio = (qty / maxQty).toFloat().coerceIn(0.1f, 1f)
                            when {
                                ratio < 0.20f -> Color(0xFF043424) // Dark Forest
                                ratio < 0.45f -> Color(0xFF065F46) // Mid Dark Green
                                ratio < 0.75f -> Color(0xFF059669) // Mid Bright Green
                                else -> Color(0xFF10B981) // High Volume Bright Green
                            }
                        }
                    }

                    // Determine Cell Border
                    val cellBorder = when {
                        isSelected -> androidx.compose.foundation.BorderStroke(2.dp, PosGreenActive)
                        isFirstPrize -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
                        isSecondPrize -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        isThirdPrize -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF97316))
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cellBgColor)
                            .then(
                                if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable { onNumberClick(numStr) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = numStr,
                                fontSize = 10.sp,
                                fontWeight = if (qty > 0.0 || isFirstPrize || isSecondPrize || isThirdPrize) FontWeight.Black else FontWeight.Bold,
                                color = when {
                                    isFirstPrize -> Color(0xFFFBBF24)
                                    isSecondPrize -> Color(0xFFE2E8F0)
                                    isThirdPrize -> Color(0xFFFB923C)
                                    qty > 0.0 -> if ((qty / maxQty) >= 0.75f) Color(0xFF022C22) else Color(0xFF34D399)
                                    else -> Color(0xFF334155)
                                },
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 10.sp
                            )

                            if (qty > 0.0) {
                                Text(
                                    text = "$qty",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if ((qty / maxQty) >= 0.75f) Color(0xFF022C22) else Color(0xFF6EE7B7),
                                    lineHeight = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// VOLUME GRADIENT LEGEND
// ----------------------------------------------------------------------
@Composable
private fun VolumeGradientLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "MENOS VOLUMEN",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = PosTextSecondary,
            letterSpacing = 0.3.sp
        )

        Box(
            modifier = Modifier
                .width(130.dp)
                .height(3.5.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF043424),
                            Color(0xFF065F46),
                            Color(0xFF059669),
                            Color(0xFF10B981)
                        )
                    )
                )
        )

        Text(
            text = "MÁS VOLUMEN",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = PosTextSecondary,
            letterSpacing = 0.3.sp
        )
    }
}

// ----------------------------------------------------------------------
// COMBINACIONES SECTION (Pales / Pares)
// ----------------------------------------------------------------------
@Composable
private fun CombinacionesSection(
    itemsForDraw: List<SaleItem>,
    selectedKey: String?,
    onComboClick: (String) -> Unit
) {
    val paleItems = remember(itemsForDraw) {
        itemsForDraw.filter { it.modality == "PALE" || it.number.contains("-") }
    }

    val comboGroups = remember(paleItems) {
        paleItems.groupBy { it.number }
            .map { (number, items) ->
                Triple(number, items.sumOf { it.quantity }, items.sumOf { it.total })
            }
    }

    val selectedCombo = if (selectedKey?.startsWith("PALE:") == true) {
        selectedKey.substringAfter("PALE:")
    } else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "COMBINACIONES VENDIDAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PosTextSecondary,
            letterSpacing = 0.5.sp
        )

        if (comboGroups.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E18)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO HAY COMBINACIONES VENDIDAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextDisabled
                    )
                }
            }
        } else {
            // Render 2 cards per row
            val chunked = comboGroups.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (comboNumber, qty, totalAmt) ->
                        val isSelected = selectedCombo == comboNumber
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onComboClick(comboNumber) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1A)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PosGreenActive else PosBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = comboNumber,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PosTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "x$qty",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextSecondary
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", totalAmt)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PosGreenActive
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// BILLETES SECTION (3-4 Dígitos)
// ----------------------------------------------------------------------
@Composable
private fun BilletesSection(
    itemsForDraw: List<SaleItem>,
    selectedKey: String?,
    onBilleteClick: (String) -> Unit
) {
    val billeteItems = remember(itemsForDraw) {
        itemsForDraw.filter { it.digits > 2 && !it.number.contains("-") && it.modality != "PALE" }
    }

    val billeteGroups = remember(billeteItems) {
        billeteItems.groupBy { it.number }
            .map { (number, items) ->
                Triple(number, items.sumOf { it.quantity }, items.sumOf { it.total })
            }
    }

    val selectedBillete = if (selectedKey?.startsWith("BILLETE:") == true) {
        selectedKey.substringAfter("BILLETE:")
    } else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "BILLETES VENDIDOS",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PosTextSecondary,
            letterSpacing = 0.5.sp
        )

        if (billeteGroups.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E18)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO HAY BILLETES VENDIDOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextDisabled
                    )
                }
            }
        } else {
            val chunked = billeteGroups.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (billeteNumber, qty, totalAmt) ->
                        val isSelected = selectedBillete == billeteNumber
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onBilleteClick(billeteNumber) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1A)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PosGreenActive else PosBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = billeteNumber,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PosTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "x$qty",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextSecondary
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", totalAmt)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PosGreenAction
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
