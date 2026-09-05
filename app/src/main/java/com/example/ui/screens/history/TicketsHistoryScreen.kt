package com.example.ui.screens.history

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SaleWithItems
import com.example.data.model.Draw
import com.example.data.model.SaleItem
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PrizeCalculator
import com.example.util.ThermalReceiptHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsHistoryScreen(
    viewModel: PosViewModel,
    onNavigateToSales: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val salesWithItems by viewModel.sales.collectAsState()
    val draws by viewModel.draws.collectAsState()
    val resultsMap by viewModel.resultsMap.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser.role == "ADMINISTRADOR" || currentUser.role == "SUPERVISOR"

    var searchQuery by remember { mutableStateOf("") }
    var selectedDrawFilterId by remember { mutableStateOf<String?>(null) }
    var selectedModalityTab by remember { mutableStateOf("TODO") } // "TODO", "CHANCE", "BILLETE", "PALÉ"
    var showDrawFilterDropdown by remember { mutableStateOf(false) }

    // Expanded accordion Sorteos (keep track of which draws are currently open)
    val expandedDrawIds = remember { mutableStateListOf<String>() }

    // Dialog states
    var selectedSaleForReceipt by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToVoid by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToEditCustomer by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToRepeatTarget by remember { mutableStateOf<Pair<SaleWithItems, String>?>(null) } // Pair(saleWithItems, drawId)

    // Date formatter for display
    val timeFormat = remember { SimpleDateFormat("h:mm:ss a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }

    // Modality filter mapping
    val modalityFilter = remember(selectedModalityTab) {
        when (selectedModalityTab) {
            "CHANCE" -> "CHANCE"
            "BILLETE" -> "BILLETE"
            "PALÉ", "PALE" -> "PALE"
            else -> null
        }
    }

    // Filtered sales matching search query, draw filter, and modality
    val filteredSales = remember(salesWithItems, searchQuery, selectedDrawFilterId, modalityFilter) {
        salesWithItems.filter { saleItem ->
            val sale = saleItem.sale
            val items = saleItem.items

            val matchesSearch = searchQuery.isBlank() ||
                    sale.ticketNumber.contains(searchQuery, ignoreCase = true) ||
                    sale.customerName.contains(searchQuery, ignoreCase = true) ||
                    sale.userName.contains(searchQuery, ignoreCase = true) ||
                    sale.id.contains(searchQuery, ignoreCase = true) ||
                    items.any { it.number.contains(searchQuery) }

            val matchesDraw = selectedDrawFilterId == null ||
                    items.any { it.drawId == selectedDrawFilterId }

            val matchesModality = modalityFilter == null ||
                    items.any { it.modality.equals(modalityFilter, ignoreCase = true) }

            matchesSearch && matchesDraw && matchesModality
        }
    }

    // Calculate Global Resumen Metrics
    val activeSales = remember(salesWithItems, modalityFilter, selectedDrawFilterId) {
        salesWithItems.filter { it.sale.status == "ACTIVA" }
    }

    val globalTotalSold = remember(activeSales, modalityFilter, selectedDrawFilterId) {
        activeSales.flatMap { it.items }
            .filter { item ->
                (selectedDrawFilterId == null || item.drawId == selectedDrawFilterId) &&
                        (modalityFilter == null || item.modality.equals(modalityFilter, ignoreCase = true))
            }
            .sumOf { it.total }
    }

    val globalTotalCommission = remember(activeSales, modalityFilter, selectedDrawFilterId) {
        globalTotalSold * 0.15 // 15% standard commission
    }

    val globalTotalPrizes = remember(activeSales, resultsMap, modalityFilter, selectedDrawFilterId) {
        activeSales.flatMap { it.items }
            .filter { item ->
                (selectedDrawFilterId == null || item.drawId == selectedDrawFilterId) &&
                        (modalityFilter == null || item.modality.equals(modalityFilter, ignoreCase = true))
            }
            .sumOf { item ->
                val res = resultsMap[item.drawId]
                PrizeCalculator.calculateItemPrize(item, res)
            }
    }

    val globalUtility = remember(globalTotalSold, globalTotalCommission, globalTotalPrizes) {
        globalTotalSold - globalTotalCommission - globalTotalPrizes
    }

    // Group sales by Draw
    val drawsListToDisplay = remember(draws, filteredSales, selectedDrawFilterId) {
        val drawsWithSalesIds = filteredSales.flatMap { it.items }.map { it.drawId }.toSet()
        val allDrawsMap = draws.associateBy { it.id }

        // Get draws that have sales or matches filter
        val result = mutableListOf<Draw>()
        draws.forEach { draw ->
            if (selectedDrawFilterId != null) {
                if (draw.id == selectedDrawFilterId) result.add(draw)
            } else if (drawsWithSalesIds.contains(draw.id)) {
                result.add(draw)
            }
        }

        // If empty (e.g. initial view before sales or search), show all draws
        if (result.isEmpty()) {
            if (selectedDrawFilterId != null) {
                allDrawsMap[selectedDrawFilterId]?.let { result.add(it) }
            } else {
                result.addAll(draws)
            }
        }
        result
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==========================================
        // SECTION 1: FILTROS DE HISTORIAL
        // ==========================================
        item(key = "filters_section") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PosPanel,
                border = BorderStroke(1.dp, PosBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "FILTROS DE HISTORIAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Draw Filter Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentDrawFilterName = remember(selectedDrawFilterId, draws) {
                            if (selectedDrawFilterId == null) "Todos los Sorteos"
                            else draws.find { it.id == selectedDrawFilterId }?.let { "${it.icon} ${it.name}" } ?: "Sorteo Seleccionado"
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDrawFilterDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            color = PosBackgroundSecondary,
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "#",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PosGreenActive
                                    )
                                    Text(
                                        text = currentDrawFilterName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextPrimary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Seleccionar sorteo",
                                    tint = PosTextSecondary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showDrawFilterDropdown,
                            onDismissRequest = { showDrawFilterDropdown = false },
                            modifier = Modifier
                                .background(PosPanelSecondary)
                                .border(1.dp, PosBorder, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "# Todos los Sorteos",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedDrawFilterId == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedDrawFilterId == null) PosGreenActive else PosTextPrimary
                                    )
                                },
                                onClick = {
                                    selectedDrawFilterId = null
                                    showDrawFilterDropdown = false
                                }
                            )
                            Divider(color = PosBorder)
                            draws.forEach { draw ->
                                val isSelected = selectedDrawFilterId == draw.id
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${draw.icon} ${draw.name} (${draw.drawTime})",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PosGreenActive else PosTextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedDrawFilterId = draw.id
                                        showDrawFilterDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Search Client or ID Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Buscar cliente o ID...",
                                fontSize = 13.sp,
                                color = PosTextDisabled
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = PosTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        tint = PosTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PosBackgroundSecondary,
                            unfocusedContainerColor = PosBackgroundSecondary,
                            focusedBorderColor = PosGreenAction,
                            unfocusedBorderColor = PosBorder,
                            focusedTextColor = PosTextPrimary,
                            unfocusedTextColor = PosTextPrimary
                        )
                    )
                }
            }
        }

        // ==========================================
        // SECTION 2: RESUMEN GLOBAL (METRICS + TABS)
        // ==========================================
        item(key = "resumen_global_section") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header + Modality Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RESUMEN GLOBAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Modality Tabs: TODO, CHANCE, BILLETE, PALÉ
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf("TODO", "CHANCE", "BILLETE", "PALÉ")
                        tabs.forEach { tab ->
                            val isSelected = selectedModalityTab.equals(tab, ignoreCase = true)
                            Surface(
                                modifier = Modifier.clickable { selectedModalityTab = tab },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) PosGreenAction else Color.Transparent,
                                border = if (isSelected) null else BorderStroke(0.5.dp, PosBorder)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) PosBackground else PosTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 4-Card Metric Grid (2x2)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: VENDIDO & COMISIÓN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // VENDIDO
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = PosPanel,
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "VENDIDO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", globalTotalSold)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = Color.White
                                )
                            }
                        }

                        // COMISIÓN
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = PosPanel,
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "COMISIÓN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", globalTotalCommission)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }

                    // Row 2: PREMIOS & UTILIDAD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PREMIOS
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = PosPanel,
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "PREMIOS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", globalTotalPrizes)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }

                        // UTILIDAD
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = PosPanel,
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "UTILIDAD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextSecondary
                                    )
                                    Text(text = "🏆", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", globalUtility)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = if (globalUtility >= 0) Color(0xFF00E676) else Color(0xFFFF5252)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3: SORTEOS LIST (ACCORDION GROUPED)
        // ==========================================
        if (drawsListToDisplay.isEmpty()) {
            item(key = "empty_state") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PosPanel,
                    border = BorderStroke(1.dp, PosBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PosTextDisabled,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "No hay registros para los filtros seleccionados",
                                fontSize = 13.sp,
                                color = PosTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(drawsListToDisplay, key = { it.id }) { draw ->
                val isExpanded = expandedDrawIds.contains(draw.id)
                val drawResult = resultsMap[draw.id]

                // All sales that contain plays for this draw
                val salesForThisDraw = remember(filteredSales, draw.id) {
                    filteredSales.filter { saleWithItem ->
                        saleWithItem.items.any { it.drawId == draw.id }
                    }
                }

                // Total sales and prizes for this draw
                val drawTotalSales = remember(salesWithItems, draw.id, modalityFilter) {
                    PrizeCalculator.calculateDrawSales(draw.id, salesWithItems, modalityFilter)
                }

                val drawTotalPrizes = remember(salesWithItems, draw.id, resultsMap, modalityFilter) {
                    PrizeCalculator.calculateDrawPrizes(draw.id, salesWithItems, resultsMap, modalityFilter)
                }

                val isDrawCurrentlyOpen = remember(draw, drawResult) {
                    if (!draw.active) {
                        false
                    } else if (drawResult != null && (drawResult.firstPrize.isNotBlank() || drawResult.secondPrize.isNotBlank() || drawResult.thirdPrize.isNotBlank())) {
                        false
                    } else {
                        try {
                            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                            val now = Calendar.getInstance()
                            val currentStr = SimpleDateFormat("hh:mm a", Locale.US).format(now.time)
                            val parsedClose = sdf.parse(draw.closingTime)
                            val parsedNow = sdf.parse(currentStr)
                            if (parsedClose != null && parsedNow != null) {
                                val calClose = Calendar.getInstance().apply { time = parsedClose }
                                val calNow = Calendar.getInstance().apply { time = parsedNow }
                                !calNow.after(calClose)
                            } else {
                                true
                            }
                        } catch (e: Exception) {
                            true
                        }
                    }
                }

                // RED CARD RULE: El color rojo solo se aplica cuando el total de premios supere la utilidad de ese sorteo
                val drawTotalCommission = remember(drawTotalSales) { drawTotalSales * 0.15 }
                val drawGrossUtility = drawTotalSales - drawTotalCommission
                val isOverpaid = drawResult != null && drawTotalPrizes > drawGrossUtility && drawTotalPrizes > 0.0

                // Winning numbers for this draw
                val winningNumbers = remember(drawResult) {
                    listOfNotNull(
                        drawResult?.firstPrize?.takeIf { it.isNotBlank() },
                        drawResult?.secondPrize?.takeIf { it.isNotBlank() },
                        drawResult?.thirdPrize?.takeIf { it.isNotBlank() }
                    )
                }

                // Determine daytime or nighttime emoji
                val timeEmoji = remember(draw.drawTime) {
                    if (draw.drawTime.contains("PM", ignoreCase = true) &&
                        (draw.drawTime.startsWith("6") || draw.drawTime.startsWith("7") ||
                                draw.drawTime.startsWith("8") || draw.drawTime.startsWith("9") ||
                                draw.drawTime.startsWith("10") || draw.drawTime.startsWith("11"))
                    ) {
                        "🌙"
                    } else {
                        "☀️"
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ----------------------------------------------------
                    // Sorteo Accordion Header Card
                    // ----------------------------------------------------
                    val headerContainerColor = when {
                        isOverpaid -> Color(0xFFE53935) // Red Alert Card!
                        isExpanded -> PosPanel
                        else -> PosPanel
                    }

                    val headerBorder = when {
                        isOverpaid -> BorderStroke(1.dp, Color(0xFFFF5252))
                        isExpanded -> BorderStroke(1.dp, PosGreenAction)
                        else -> BorderStroke(1.dp, PosBorder)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isExpanded) {
                                    expandedDrawIds.remove(draw.id)
                                } else {
                                    expandedDrawIds.add(draw.id)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = headerContainerColor,
                        border = headerBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Column: Chevron + Calendar + Flag + Title + Results Pills + Date
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Chevron Indicator
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                    tint = if (isOverpaid) Color.White else if (isExpanded) PosGreenAction else PosTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )

                                // Calendar Icon
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isOverpaid) Color.White else if (isExpanded) PosGreenAction else PosGreenActive,
                                    modifier = Modifier.size(16.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    // Sorteo Name & Badges Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${draw.icon} ${draw.drawTime} ${draw.name} $timeEmoji",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (drawTotalPrizes > 0.0) {
                                            Text(text = "🏆", fontSize = 11.sp)
                                        }

                                        // Lock Icon ONLY when the draw is actually closed!
                                        if (!isDrawCurrentlyOpen) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Sorteo cerrado",
                                                tint = if (isOverpaid) Color.White.copy(alpha = 0.9f) else PosTextSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    // Date + Winning Numbers Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = drawResult?.drawDate?.takeIf { it.isNotBlank() } ?: dateFormat.format(Date()),
                                            fontSize = 11.sp,
                                            color = if (isOverpaid) Color.White.copy(alpha = 0.9f) else PosTextSecondary
                                        )

                                        // Winning Result Pills
                                        if (winningNumbers.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                winningNumbers.forEach { winNum ->
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isOverpaid) Color(0xFF8E0000) else PosGold.copy(alpha = 0.2f),
                                                        border = BorderStroke(
                                                            0.5.dp,
                                                            if (isOverpaid) Color(0xFFFFD54F) else PosGold
                                                        )
                                                    ) {
                                                        Text(
                                                            text = winNum,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isOverpaid) Color(0xFFFFD54F) else PosGold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Right Column: Sales Badge & Prizes Awarded
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Sales Total Pill Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isOverpaid) Color(0xFFB71C1C) else PosPanelSecondary,
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isOverpaid) Color.White.copy(alpha = 0.4f) else PosBorder
                                    )
                                ) {
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", drawTotalSales)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }

                                // Total Prizes Label underneath sales badge
                                if (drawTotalPrizes > 0.0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(text = "🏆", fontSize = 11.sp)
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", drawTotalPrizes)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isOverpaid) Color(0xFFFFD54F) else PosGold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // Expanded Tickets / Sales List for this Sorteo
                    // ----------------------------------------------------
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val pageSize = 5
                        val drawPageMap = remember { mutableStateMapOf<String, Int>() }
                        val totalPages = remember(salesForThisDraw.size) {
                            ((salesForThisDraw.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                        }
                        val currentPage = (drawPageMap[draw.id] ?: 1).coerceIn(1, totalPages)
                        val pagedSales = remember(salesForThisDraw, currentPage, pageSize) {
                            salesForThisDraw.drop((currentPage - 1) * pageSize).take(pageSize)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (salesForThisDraw.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = PosPanelSecondary,
                                    border = BorderStroke(1.dp, PosBorder)
                                ) {
                                    Text(
                                        text = "No hay jugadas registradas para este sorteo",
                                        fontSize = 12.sp,
                                        color = PosTextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                // Top Pagination Controls if more than 1 page
                                if (totalPages > 1) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Button(
                                            onClick = {
                                                if (currentPage > 1) {
                                                    drawPageMap[draw.id] = currentPage - 1
                                                }
                                            },
                                            enabled = currentPage > 1,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                disabledContainerColor = Color(0xFF1E293B)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Anterior",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Anterior",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "PÁGINA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PosTextSecondary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF0F172A),
                                                border = BorderStroke(0.5.dp, PosBorder)
                                            ) {
                                                Text(
                                                    text = "$currentPage",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "de $totalPages",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PosTextSecondary
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (currentPage < totalPages) {
                                                    drawPageMap[draw.id] = currentPage + 1
                                                }
                                            },
                                            enabled = currentPage < totalPages,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                disabledContainerColor = Color(0xFF1E293B)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "Siguiente",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Siguiente",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                // Tickets List (Paged)
                                pagedSales.forEach { saleWithItem ->
                                    val sale = saleWithItem.sale
                                    val isAnulada = sale.status == "ANULADA"

                                    // Filter plays belonging to this draw
                                    val drawPlays = saleWithItem.items.filter { it.drawId == draw.id }
                                    val drawSubtotal = drawPlays.sumOf { it.total }

                                    // Calculate total won by this ticket in this draw
                                    val totalTicketPrize = if (isAnulada) 0.0 else drawPlays.sumOf { play ->
                                        PrizeCalculator.calculateItemPrize(play, drawResult)
                                    }
                                    val isTicketWon = totalTicketPrize > 0.0

                                    // Unique independent TX identifier
                                    val drawTxCode = "TX: ${sale.id.take(8).uppercase()}-${draw.id.hashCode().toString().takeLast(4)}"

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isAnulada) PosBackgroundSecondary.copy(alpha = 0.5f) else Color(0xFF131826),
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isAnulada -> PosError.copy(alpha = 0.4f)
                                                isTicketWon -> Color(0xFFEF4444).copy(alpha = 0.7f)
                                                else -> Color(0xFF222C3E)
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Top Row: Left (Ticket ID + Layers icon + Won badge) | Right (Winning numbers table + Subtotal)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Left: Ticket ID & Won Badge
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = sale.ticketNumber,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = if (isTicketWon) Color(0xFFEF4444) else Color.White
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Outlined.Layers,
                                                            contentDescription = null,
                                                            tint = Color(0xFF60A5FA),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    if (isTicketWon) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFDC2626)
                                                        ) {
                                                            Text(
                                                                text = "GANÓ: $${String.format(Locale.US, "%.2f", totalTicketPrize)}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else if (sale.customerName.isNotBlank() && sale.customerName != "Cliente General") {
                                                        Text(
                                                            text = sale.customerName,
                                                            fontSize = 11.sp,
                                                            color = PosTextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                // Right: Winning Numbers mini header & Draw Subtotal (Green)
                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    if (winningNumbers.isNotEmpty()) {
                                                        Text(
                                                            text = "1er   2do   3er",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = PosTextSecondary
                                                        )
                                                        Text(
                                                            text = winningNumbers.joinToString("    "),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = PosGold
                                                        )
                                                    }

                                                    Text(
                                                        text = "$${String.format(Locale.US, "%.2f", drawSubtotal)}",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (isAnulada) PosTextDisabled else PosGreenAction
                                                    )
                                                }
                                            }

                                            // Action Buttons Row: Share, Repeat, Edit, Delete, Mini-Receipt
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // 1. Share Ticket
                                                IconButton(
                                                    onClick = {
                                                        val singleDrawItems = saleWithItem.items.filter { it.drawId == draw.id }
                                                        val singleDrawSale = saleWithItem.sale.copy(
                                                            subtotal = singleDrawItems.sumOf { it.total },
                                                            total = singleDrawItems.sumOf { it.total }
                                                        )
                                                        val singleDrawSaleWithItems = SaleWithItems(
                                                            sale = singleDrawSale,
                                                            items = singleDrawItems
                                                        )
                                                        selectedSaleForReceipt = singleDrawSaleWithItems
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Share,
                                                        contentDescription = "Compartir ticket",
                                                        tint = PosTextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // 2. Repeat / Copy plays to another draw
                                                IconButton(
                                                    onClick = {
                                                        saleToRepeatTarget = Pair(saleWithItem, draw.id)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Repeat,
                                                        contentDescription = "Repetir jugadas a otro sorteo",
                                                        tint = PosGreenAction,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // 3. Edit Ticket in Sales POS - ONLY VISIBLE IF DRAW IS OPEN & ACTIVE
                                                if (!isAnulada && isDrawCurrentlyOpen) {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.startEditingSale(saleWithItem)
                                                            onNavigateToSales()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Edit,
                                                            contentDescription = "Editar ticket",
                                                            tint = PosTextSecondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                // 4. Delete / Anular Ticket - ONLY VISIBLE IF DRAW IS OPEN & ADMIN
                                                if (isAdmin && !isAnulada && isDrawCurrentlyOpen) {
                                                    IconButton(
                                                        onClick = { saleToVoid = saleWithItem },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = "Anular ticket",
                                                            tint = PosTextSecondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                // 5. Mini Ticket Receipt Icon for MULTI-DRAW tickets
                                                val distinctDrawsCount = remember(saleWithItem) {
                                                    saleWithItem.items.map { it.drawId }.distinct().size
                                                }
                                                if (distinctDrawsCount > 1) {
                                                    IconButton(
                                                        onClick = {
                                                            selectedSaleForReceipt = saleWithItem
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ReceiptLong,
                                                            contentDescription = "Ver ticket completo multi-sorteo",
                                                            tint = Color(0xFF60A5FA),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                if (isAnulada) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = PosError.copy(alpha = 0.2f),
                                                        border = BorderStroke(0.5.dp, PosError)
                                                    ) {
                                                        Text(
                                                            text = "ANULADO",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PosError,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Timestamp & TX Code
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = PosTextDisabled,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = timeFormat.format(Date(sale.createdAt)),
                                                    fontSize = 11.sp,
                                                    color = PosTextSecondary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = drawTxCode,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PosTextSecondary
                                                )
                                            }

                                            // Plays 2-Column Grid
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                drawPlays.chunked(2).forEach { rowPlays ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        rowPlays.forEach { play ->
                                                            val playPrize = if (isAnulada) 0.0 else PrizeCalculator.calculateItemPrize(play, drawResult)
                                                            val isPlayWon = playPrize > 0.0

                                                            Surface(
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = if (isPlayWon) Color(0xFF2B2010) else Color(0xFF161E2E),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (isPlayWon) PosGold else Color(0xFF263248)
                                                                )
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = play.number,
                                                                                fontSize = 13.sp,
                                                                                fontWeight = FontWeight.ExtraBold,
                                                                                fontFamily = FontFamily.Monospace,
                                                                                color = if (isPlayWon) PosGold else Color.White
                                                                            )
                                                                            Text(
                                                                                text = "x${play.quantity}",
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = PosTextSecondary
                                                                            )
                                                                        }
                                                                        Text(
                                                                            text = "${draw.icon} ${draw.name.take(7)}...",
                                                                            fontSize = 10.sp,
                                                                            color = PosTextSecondary,
                                                                            maxLines = 1
                                                                        )
                                                                    }

                                                                    Column(
                                                                        horizontalAlignment = Alignment.End,
                                                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "$${String.format(Locale.US, "%.2f", play.total)}",
                                                                            fontSize = 12.5.sp,
                                                                            fontWeight = FontWeight.ExtraBold,
                                                                            fontFamily = FontFamily.Monospace,
                                                                            color = if (isAnulada) PosTextDisabled else Color.White
                                                                        )
                                                                        if (isPlayWon) {
                                                                            Text(
                                                                                text = "+$${String.format(Locale.US, "%.2f", playPrize)}",
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.ExtraBold,
                                                                                fontFamily = FontFamily.Monospace,
                                                                                color = PosGold
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (rowPlays.size == 1) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom Pagination Controls if more than 1 page
                                if (totalPages > 1) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Button(
                                            onClick = {
                                                if (currentPage > 1) {
                                                    drawPageMap[draw.id] = currentPage - 1
                                                }
                                            },
                                            enabled = currentPage > 1,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                disabledContainerColor = Color(0xFF1E293B)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Anterior",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Anterior",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "PÁGINA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PosTextSecondary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF0F172A),
                                                border = BorderStroke(0.5.dp, PosBorder)
                                            ) {
                                                Text(
                                                    text = "$currentPage",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "de $totalPages",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PosTextSecondary
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (currentPage < totalPages) {
                                                    drawPageMap[draw.id] = currentPage + 1
                                                }
                                            },
                                            enabled = currentPage < totalPages,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                disabledContainerColor = Color(0xFF1E293B)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "Siguiente",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Siguiente",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
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

    // ==========================================
    // DIALOGS & MODALS
    // ==========================================

    // 1. Receipt Modal
    selectedSaleForReceipt?.let { saleWithItemsDetail ->
        ReceiptDialog(
            saleWithItems = saleWithItemsDetail,
            resultsMap = resultsMap,
            onDismiss = { selectedSaleForReceipt = null },
            onNewSale = {
                viewModel.repeatSaleToCart(saleWithItemsDetail)
                selectedSaleForReceipt = null
            }
        )
    }

    // 2. Void Confirmation Dialog
    saleToVoid?.let { toVoid ->
        var reason by remember { mutableStateOf("Error de captura") }

        AlertDialog(
            onDismissRequest = { saleToVoid = null },
            containerColor = PosBackgroundSecondary,
            titleContentColor = PosTextPrimary,
            textContentColor = PosTextSecondary,
            title = { Text("Anular Ticket #${toVoid.sale.ticketNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Confirmas la anulación de este ticket de ${toVoid.sale.customerName} por un total de $${String.format(Locale.US, "%.2f", toVoid.sale.total)}?")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo de anulación") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PosPanel,
                            unfocusedContainerColor = PosPanel,
                            focusedBorderColor = PosError,
                            unfocusedBorderColor = PosBorder,
                            focusedTextColor = PosTextPrimary,
                            unfocusedTextColor = PosTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidSale(toVoid.sale.id, reason)
                        saleToVoid = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosError)
                ) {
                    Text("Anular Ticket", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToVoid = null }) {
                    Text("Cancelar", color = PosTextSecondary)
                }
            }
        )
    }

    // 3. Edit Ticket Customer Name Dialog
    saleToEditCustomer?.let { toEdit ->
        var customerInput by remember { mutableStateOf(toEdit.sale.customerName) }

        AlertDialog(
            onDismissRequest = { saleToEditCustomer = null },
            containerColor = PosBackgroundSecondary,
            titleContentColor = PosTextPrimary,
            textContentColor = PosTextSecondary,
            title = { Text("Editar Cliente - Ticket #${toEdit.sale.ticketNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Modifica el nombre del cliente para este comprobante:")
                    OutlinedTextField(
                        value = customerInput,
                        onValueChange = { customerInput = it },
                        label = { Text("Nombre del cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PosPanel,
                            unfocusedContainerColor = PosPanel,
                            focusedBorderColor = PosGreenAction,
                            unfocusedBorderColor = PosBorder,
                            focusedTextColor = PosTextPrimary,
                            unfocusedTextColor = PosTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerInput.isNotBlank()) {
                            viewModel.updateSaleCustomer(toEdit.sale.id, customerInput)
                        }
                        saleToEditCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Text("Guardar", color = PosBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToEditCustomer = null }) {
                    Text("Cancelar", color = PosTextSecondary)
                }
            }
        )
    }

    // 4. Repeat / Copy Ticket Plays to Another Draw Dialog
    saleToRepeatTarget?.let { (saleWithItemToRepeat, currentDrawId) ->
        val playsToCopy = remember(saleWithItemToRepeat, currentDrawId) {
            saleWithItemToRepeat.items.filter { it.drawId == currentDrawId }
        }
        var selectedTargetDrawId by remember {
            mutableStateOf(currentDrawId)
        }

        AlertDialog(
            onDismissRequest = { saleToRepeatTarget = null },
            containerColor = PosBackgroundSecondary,
            titleContentColor = PosTextPrimary,
            textContentColor = PosTextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = PosGreenAction,
                        modifier = Modifier.size(22.dp)
                    )
                    Text("Repetir Jugadas a Sorteo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecciona el sorteo al que deseas copiar las ${playsToCopy.size} jugadas del ticket #${saleWithItemToRepeat.sale.ticketNumber}:",
                        fontSize = 13.sp,
                        color = PosTextSecondary
                    )

                    // Summary of plays being copied
                    Surface(
                        color = PosPanel,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            playsToCopy.take(4).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.modality} #${item.number} x${item.quantity}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextPrimary
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", item.total)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = PosGreenAction
                                    )
                                }
                            }
                            if (playsToCopy.size > 4) {
                                Text(
                                    text = "+ ${playsToCopy.size - 4} jugadas más...",
                                    fontSize = 11.sp,
                                    color = PosTextSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = "Sorteo Destino:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PosTextPrimary
                    )

                    // Draw list selector
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(draws.filter { it.active }) { targetDraw ->
                            val isSelected = targetDraw.id == selectedTargetDrawId
                            Surface(
                                onClick = { selectedTargetDrawId = targetDraw.id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PosGreenAction.copy(alpha = 0.15f) else PosPanel,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PosGreenAction else PosBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = targetDraw.icon.ifBlank { "🎯" },
                                            fontSize = 16.sp
                                        )
                                        Column {
                                            Text(
                                                text = targetDraw.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) PosGreenAction else PosTextPrimary
                                            )
                                            Text(
                                                text = "${targetDraw.drawTime} (Cierre: ${targetDraw.closingTime})",
                                                fontSize = 11.sp,
                                                color = PosTextSecondary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Seleccionado",
                                            tint = PosGreenAction,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetDraw = draws.find { it.id == selectedTargetDrawId }
                        // Create a subset sale with only plays of this draw
                        val filteredSaleWithItems = saleWithItemToRepeat.copy(
                            items = playsToCopy
                        )
                        viewModel.repeatSaleToCart(filteredSaleWithItems, targetDraw)
                        saleToRepeatTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Text("Cargar al Carrito", color = PosBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToRepeatTarget = null }) {
                    Text("Cancelar", color = PosTextSecondary)
                }
            }
        )
    }
}
