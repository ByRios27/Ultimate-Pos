package com.example.ui.screens.other

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SaleWithItems
import com.example.data.model.DrawResult
import com.example.data.model.SaleItem
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PrizeCalculator
import java.text.SimpleDateFormat
import java.util.*

enum class ArchiveDateFilterType(val displayName: String) {
    HOY("Hoy"),
    AYER("Ayer"),
    ESTA_SEMANA("Esta Semana"),
    ESTE_MES("Este Mes"),
    TODO("Todo"),
    RANGO_PERSONALIZADO("Rango Personalizado")
}

private val ArchivePurple = Color(0xFFA855F7)
private val ArchivePurpleDark = Color(0xFF1E1638)
private val ArchivePurpleBorder = Color(0xFF7C3AED)

@Composable
fun ArchivosScreen(
    viewModel: PosViewModel,
    onNavigateToTickets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val salesWithItems by viewModel.sales.collectAsState()
    val resultsMap by viewModel.resultsMap.collectAsState()

    var selectedFilterType by remember { mutableStateOf(ArchiveDateFilterType.TODO) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Custom date range state (in milliseconds)
    var customStartDate by remember {
        val cal = getStartOfDayCalendar()
        cal.add(Calendar.DAY_OF_MONTH, -7)
        mutableStateOf(cal.timeInMillis)
    }
    var customEndDate by remember {
        val cal = getEndOfDayCalendar()
        mutableStateOf(cal.timeInMillis)
    }

    // Drilldown navigation hierarchy:
    // Level 0: null (Shows Years list)
    // Level 1: selectedYear != null (Shows Months list)
    // Level 2: selectedMonth != null (Shows Days list + 4 KPIs)
    // Level 3: selectedDay != null (Shows Draws list of that day + 4 KPIs)
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) } // 1-12
    var selectedMonthName by remember { mutableStateOf<String?>(null) }
    var selectedDayStr by remember { mutableStateOf<String?>(null) } // "dd/MM/yyyy"

    // Detail modal for a specific draw on a selected day
    var detailedDrawId by remember { mutableStateOf<String?>(null) }

    // Filter active sales according to selected date filter
    val filteredSales = remember(salesWithItems, selectedFilterType, customStartDate, customEndDate) {
        val activeSales = salesWithItems.filter { it.sale.status == "ACTIVA" }
        when (selectedFilterType) {
            ArchiveDateFilterType.HOY -> {
                val start = getStartOfDayCalendar().timeInMillis
                val end = getEndOfDayCalendar().timeInMillis
                activeSales.filter { it.sale.createdAt in start..end }
            }
            ArchiveDateFilterType.AYER -> {
                val calStart = getStartOfDayCalendar().apply { add(Calendar.DAY_OF_MONTH, -1) }
                val calEnd = getEndOfDayCalendar().apply { add(Calendar.DAY_OF_MONTH, -1) }
                activeSales.filter { it.sale.createdAt in calStart.timeInMillis..calEnd.timeInMillis }
            }
            ArchiveDateFilterType.ESTA_SEMANA -> {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                activeSales.filter { it.sale.createdAt in start..end }
            }
            ArchiveDateFilterType.ESTE_MES -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                activeSales.filter { it.sale.createdAt in start..end }
            }
            ArchiveDateFilterType.TODO -> activeSales
            ArchiveDateFilterType.RANGO_PERSONALIZADO -> {
                activeSales.filter { it.sale.createdAt in customStartDate..customEndDate }
            }
        }
    }

    // Helper data structures for Year -> Month -> Day -> Draw
    val archiveData = remember(filteredSales, resultsMap) {
        computeArchiveHierarchy(filteredSales, resultsMap)
    }

    val availableYears = remember(archiveData) {
        archiveData.keys.sortedDescending()
    }

    // Back Navigation Handler
    fun handleGoBack() {
        when {
            detailedDrawId != null -> detailedDrawId = null
            selectedDayStr != null -> selectedDayStr = null
            selectedMonth != null -> {
                selectedMonth = null
                selectedMonthName = null
            }
            selectedYear != null -> selectedYear = null
        }
    }

    val isDrilledDown = selectedYear != null

    if (detailedDrawId != null) {
        val dayData = archiveData[selectedYear]?.months?.get(selectedMonth)?.days?.get(selectedDayStr)
        val drawSummary = dayData?.draws?.get(detailedDrawId)
        val drawResult = resultsMap[detailedDrawId]

        if (drawSummary != null) {
            DrawDetailScreen(
                drawSummary = drawSummary,
                dateStr = selectedDayStr ?: "",
                drawResult = drawResult,
                salesWithItems = salesWithItems,
                resultsMap = resultsMap,
                onGoBack = { detailedDrawId = null },
                onNavigateToTicket = { /* Wait, we might need a general ticket viewer or pass it up */ }
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(PosBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // ====================================================
        // 1. TOP HEADER: Screen Title + Back Button (if drilled)
        // ====================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Archivo de Sorteos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PosTextPrimary
                )

                if (isDrilledDown) {
                    Surface(
                        onClick = { handleGoBack() },
                        shape = RoundedCornerShape(8.dp),
                        color = PosPanelSecondary,
                        border = BorderStroke(1.dp, PosBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = PosTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Volver",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ====================================================
        // 2. DATE FILTER SELECTOR PILL
        // ====================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showFilterDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF161B26),
                    border = BorderStroke(1.dp, PosBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = PosTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedFilterType.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Abrir filtros",
                            tint = PosTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ====================================================
        // 2B. CUSTOM DATE RANGE PICKERS (if Rango Personalizado)
        // ====================================================
        if (selectedFilterType == ArchiveDateFilterType.RANGO_PERSONALIZADO) {
            item {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Desde Button
                    Card(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customStartDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    customStartDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PosPanel),
                        border = BorderStroke(1.dp, PosBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Desde:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(customStartDate)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PosTextPrimary
                                )
                                Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = PosGreenAction, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Hasta Button
                    Card(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customEndDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, day, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    customEndDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PosPanel),
                        border = BorderStroke(1.dp, PosBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Hasta:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(customEndDate)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PosTextPrimary
                                )
                                Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = PosGreenAction, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // 3. BREADCRUMBS SECTION
        // ====================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MI ARCHIVO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArchivePurple,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.clickable {
                        selectedYear = null
                        selectedMonth = null
                        selectedMonthName = null
                        selectedDayStr = null
                    }
                )

                selectedYear?.let { yr ->
                    Text(text = " / ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                    Text(
                        text = "$yr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedMonth == null) ArchivePurple else PosTextSecondary,
                        modifier = Modifier.clickable {
                            selectedMonth = null
                            selectedMonthName = null
                            selectedDayStr = null
                        }
                    )
                }

                selectedMonthName?.let { mName ->
                    Text(text = " / ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                    Text(
                        text = mName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedDayStr == null) ArchivePurple else PosTextSecondary,
                        modifier = Modifier.clickable {
                            selectedDayStr = null
                        }
                    )
                }

                selectedDayStr?.let { dStr ->
                    Text(text = " / ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                    Text(
                        text = dStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ArchivePurple
                    )
                }
            }
        }

        // ====================================================
        // 4. LEVEL 0: YEARS LIST (Default root view)
        // ====================================================
        if (selectedYear == null) {
            if (availableYears.isEmpty()) {
                item {
                    EmptyArchiveBox(message = "No hay años con sorteos archivados.")
                }
            } else {
                items(availableYears) { year ->
                    val yearData = archiveData[year]
                    if (yearData != null) {
                        ArchiveYearCard(
                            year = year,
                            drawsCount = yearData.totalDrawsCount,
                            ticketsCount = yearData.totalTicketsCount,
                            totalSales = yearData.totalSales,
                            onClick = { selectedYear = year }
                        )
                    }
                }
            }
        }

        // ====================================================
        // 5. LEVEL 1: MONTHS LIST (Inside a Year)
        // ====================================================
        else if (selectedMonth == null) {
            val yearData = archiveData[selectedYear]
            val monthsList = yearData?.months?.keys?.sortedDescending() ?: emptyList()

            if (monthsList.isEmpty()) {
                item {
                    EmptyArchiveBox(message = "No hay meses con sorteos archivados en $selectedYear.")
                }
            } else {
                items(monthsList) { monthNum ->
                    val monthData = yearData?.months?.get(monthNum)
                    if (monthData != null) {
                        ArchiveMonthCard(
                            monthName = monthData.monthName,
                            year = selectedYear!!,
                            drawsCount = monthData.totalDrawsCount,
                            totalSales = monthData.totalSales,
                            onClick = {
                                selectedMonth = monthNum
                                selectedMonthName = "${monthData.monthName} $selectedYear"
                            }
                        )
                    }
                }
            }
        }

        // ====================================================
        // 6. LEVEL 2: DAYS LIST (Inside a Month)
        // ====================================================
        else if (selectedDayStr == null) {
            val monthData = archiveData[selectedYear]?.months?.get(selectedMonth)

            if (monthData != null) {
                // 4 KPI Cards (2x2 Grid) for this Month
                item {
                    ArchiveKpiGrid(
                        totalSales = monthData.totalSales,
                        commission = monthData.totalCommission,
                        prizes = monthData.totalPrizes,
                        utility = monthData.utility
                    )
                }

                // Section Header: DÍAS CON ACTIVIDAD
                item {
                    Text(
                        text = "DÍAS CON ACTIVIDAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                val daysList = monthData.days.keys.sortedDescending()
                if (daysList.isEmpty()) {
                    item {
                        EmptyArchiveBox(message = "No hay días con actividad en este mes.")
                    }
                } else {
                    items(daysList) { dateStr ->
                        val dayData = monthData.days[dateStr]
                        if (dayData != null) {
                            ArchiveDayCard(
                                dateStr = dateStr,
                                drawsCount = dayData.totalDrawsCount,
                                ticketsCount = dayData.totalTicketsCount,
                                totalSales = dayData.totalSales,
                                onClick = { selectedDayStr = dateStr }
                            )
                        }
                    }
                }
            } else {
                item {
                    EmptyArchiveBox(message = "No se encontraron datos para el mes seleccionado.")
                }
            }
        }

        // ====================================================
        // 7. LEVEL 3: DRAWS LIST (Inside a Day)
        // ====================================================
        else {
            val dayData = archiveData[selectedYear]?.months?.get(selectedMonth)?.days?.get(selectedDayStr)

            if (dayData != null) {
                // 4 KPI Cards (2x2 Grid) for this Day
                item {
                    ArchiveKpiGrid(
                        totalSales = dayData.totalSales,
                        commission = dayData.totalCommission,
                        prizes = dayData.totalPrizes,
                        utility = dayData.utility
                    )
                }

                // Section Header: SORTEOS DEL DÍA
                item {
                    Text(
                        text = "SORTEOS DEL DÍA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                val drawsList = dayData.draws.values.toList().sortedBy { it.drawName }
                if (drawsList.isEmpty()) {
                    item {
                        EmptyArchiveBox(message = "No hay sorteos archivados para el día $selectedDayStr.")
                    }
                } else {
                    items(drawsList) { drawSummary ->
                        ArchiveDrawItemCard(
                            drawSummary = drawSummary,
                            dateStr = selectedDayStr!!,
                            onClick = {
                                detailedDrawId = drawSummary.drawId
                            }
                        )
                    }
                }
            } else {
                item {
                    EmptyArchiveBox(message = "No se encontraron sorteos para la fecha seleccionada.")
                }
            }
        }
    }

    // ====================================================
    // DATE FILTER POPUP / DIALOG
    // ====================================================
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            confirmButton = {},
            containerColor = Color(0xFF1E222D),
            shape = RoundedCornerShape(16.dp),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    ArchiveDateFilterType.values().forEachIndexed { index, filterType ->
                        val isSelected = selectedFilterType == filterType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFilterType = filterType
                                    showFilterDialog = false
                                    // Reset drilldown when filter changes
                                    selectedYear = null
                                    selectedMonth = null
                                    selectedMonthName = null
                                    selectedDayStr = null
                                }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = filterType.displayName,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else PosTextSecondary
                            )

                            // Custom Radio Indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) PosGreenAction else PosTextDisabled,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(PosGreenAction)
                                    )
                                }
                            }
                        }

                        if (index < ArchiveDateFilterType.values().size - 1) {
                            Divider(color = PosBorder.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }
                }
            }
        )
    }
    
    } // Ends the `else { LazyColumn() { ... } ` block
}

// ====================================================
// 2x2 KPI GRID CARDS (VENTAS | COMISIÓN | PREMIOS | UTILIDAD)
// ====================================================
@Composable
fun ArchiveKpiGrid(
    totalSales: Double,
    commission: Double,
    prizes: Double,
    utility: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: VENTAS TOTALES & COMISIÓN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Ventas Totales
            ArchiveStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Tag,
                label = "VENTAS TOTALES",
                value = "$${String.format(Locale.US, "%.2f", totalSales)}",
                valueColor = Color.White
            )

            // Card 2: Comisión
            ArchiveStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.BarChart,
                label = "COMISIÓN",
                value = "$${String.format(Locale.US, "%.2f", commission)}",
                valueColor = Color(0xFFFF9800)
            )
        }

        // Row 2: PREMIOS & UTILIDAD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Premios
            ArchiveStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                label = "PREMIOS",
                value = "$${String.format(Locale.US, "%.2f", prizes)}",
                valueColor = Color(0xFFFF5252)
            )

            // Card 4: Utilidad
            ArchiveStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MonetizationOn,
                label = "UTILIDAD",
                value = "$${String.format(Locale.US, "%.2f", utility)}",
                valueColor = Color(0xFF00E676)
            )
        }
    }
}

@Composable
fun ArchiveStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2433)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PosTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}

// ====================================================
// CARDS FOR YEAR, MONTH, DAY & DRAW
// ====================================================
@Composable
fun ArchiveYearCard(
    year: Int,
    drawsCount: Int,
    ticketsCount: Int,
    totalSales: Double,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Purple calendar icon + Year title & subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ArchivePurpleDark)
                        .border(1.dp, ArchivePurpleBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = ArchivePurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "$year",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$drawsCount Sorteos • $ticketsCount Recibos",
                        fontSize = 12.5.sp,
                        color = PosTextSecondary
                    )
                }
            }

            // Right: VENTAS TOTALES + Amount + Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "VENTAS TOTALES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalSales)}",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ArchivePurple
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PosTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ArchiveMonthCard(
    monthName: String,
    year: Int,
    drawsCount: Int,
    totalSales: Double,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ArchivePurpleDark)
                        .border(1.dp, ArchivePurpleBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = ArchivePurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "$monthName $year",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$drawsCount Sorteos",
                        fontSize = 12.5.sp,
                        color = PosTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", totalSales)}",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArchivePurple
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PosTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ArchiveDayCard(
    dateStr: String,
    drawsCount: Int,
    ticketsCount: Int,
    totalSales: Double,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ArchivePurpleDark)
                        .border(1.dp, ArchivePurpleBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = ArchivePurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = dateStr,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$drawsCount Sorteos • $ticketsCount Recibos",
                        fontSize = 12.5.sp,
                        color = PosTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", totalSales)}",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArchivePurple
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PosTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ArchiveDrawItemCard(
    drawSummary: DrawArchiveSummary,
    dateStr: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon squircle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E2433))
                        .border(1.dp, PosBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (drawSummary.drawIcon.isNotBlank()) {
                        Text(text = drawSummary.drawIcon, fontSize = 20.sp)
                    } else {
                        Icon(Icons.Default.Tag, contentDescription = null, tint = PosTextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Draw Title
                    Text(
                        text = if (drawSummary.drawIcon.isNotBlank()) "${drawSummary.drawIcon} ${drawSummary.drawTime} ${drawSummary.drawName}" else "${drawSummary.drawTime} ${drawSummary.drawName}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Utility badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(0.5.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "UTILIDAD: $${String.format(Locale.US, "%.2f", drawSummary.utility)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E676),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        // Prizes badge if > 0
                        if (drawSummary.prizes > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF450A0A),
                                border = BorderStroke(0.5.dp, Color(0xFFEF4444))
                            ) {
                                Text(
                                    text = "PREMIOS: $${String.format(Locale.US, "%.2f", drawSummary.prizes)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF5252),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Subtitle
                    Text(
                        text = "$dateStr • Archivo",
                        fontSize = 11.5.sp,
                        color = PosTextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PosTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ====================================================
// DRAW DETAIL SCREEN
// ====================================================
@Composable
fun DrawDetailScreen(
    drawSummary: DrawArchiveSummary,
    dateStr: String,
    drawResult: DrawResult?,
    salesWithItems: List<SaleWithItems>,
    resultsMap: Map<String, DrawResult>,
    onGoBack: () -> Unit,
    onNavigateToTicket: (SaleWithItems) -> Unit
) {
    var showTicketModal by remember { mutableStateOf<SaleWithItems?>(null) }

    val ticketMap = remember(salesWithItems) { salesWithItems.associateBy { it.sale.id } }

    val winningNumbers = remember(drawResult) {
        if (drawResult != null) {
            listOf(drawResult.firstPrize, drawResult.secondPrize, drawResult.thirdPrize).filter { it.isNotBlank() }
        } else emptyList()
    }

    // Customer Prizes
    val prizesBySale = remember(drawSummary, drawResult) {
        val winners = mutableMapOf<String, Double>()
        if (drawResult != null) {
            for (item in drawSummary.items) {
                val prize = PrizeCalculator.calculateItemPrize(item, drawResult)
                if (prize > 0) {
                    winners[item.saleId] = (winners[item.saleId] ?: 0.0) + prize
                }
            }
        }
        winners
    }

    // Number Sales Distribution
    val salesByNumber = remember(drawSummary) {
        val map = mutableMapOf<String, Double>()
        for (item in drawSummary.items) {
            map[item.number] = (map[item.number] ?: 0.0) + item.quantity
        }
        map
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HEADER
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onGoBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Text(
                        text = if (drawSummary.drawIcon.isNotBlank()) "${drawSummary.drawIcon} ${drawSummary.drawTime} ${drawSummary.drawName}" else "${drawSummary.drawTime} ${drawSummary.drawName}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // balance center
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ArchivePurpleDark,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArchivePurple,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = PosTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CERRADO", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = PosTextSecondary)
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = PosGreenActive, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESULTADOS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = PosGreenActive)
                }
                
                Divider(color = PosBorder, modifier = Modifier.padding(top = 14.dp))
            }
        }

        // KPIs
        item {
            ArchiveKpiGrid(
                totalSales = drawSummary.sales,
                commission = drawSummary.commission,
                prizes = drawSummary.prizes,
                utility = drawSummary.utility
            )
        }

        // WINNING NUMBERS
        if (drawResult != null && winningNumbers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
                    border = BorderStroke(1.dp, PosBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NÚMEROS GANADORES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (drawResult.firstPrize.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD700)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(drawResult.firstPrize, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("1RO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                                }
                            }
                            if (drawResult.secondPrize.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0E0E0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(drawResult.secondPrize, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("2DO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                                }
                            }
                            if (drawResult.thirdPrize.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFCD7F32)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(drawResult.thirdPrize, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("3RO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // WINNERS LIST
        if (prizesBySale.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
                    border = BorderStroke(1.dp, PosBorder)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = ArchivePurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MONTO TOTAL A PAGAR POR CLIENTE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArchivePurple,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Divider(color = PosBorder)

                        prizesBySale.forEach { (saleId, prizeAmount) ->
                            val saleData = ticketMap[saleId]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ArchivePurpleDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = saleData?.sale?.customerName?.take(2)?.uppercase() ?: "CL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = saleData?.sale?.customerName ?: saleData?.sale?.ticketNumber ?: saleId,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF450A0A)
                                ) {
                                    Text(
                                        text = "A PAGAR\n$${String.format(Locale.US, "%.2f", prizeAmount)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF5252),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                IconButton(
                                    onClick = {
                                        if (saleData != null) {
                                            showTicketModal = saleData
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = "Ver Ticket", tint = ArchivePurple)
                                }
                            }
                            Divider(color = PosBorder.copy(alpha = 0.5f))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PosBackground)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Total en Premios: ", fontSize = 13.sp, color = PosTextSecondary)
                                Text("$${String.format(Locale.US, "%.2f", drawSummary.prizes)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }

        // NUMBERS DISTRIBUTION GRID
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridOn, contentDescription = null, tint = PosTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DISTRIBUCIÓN DE\nNÚMEROS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosTextSecondary,
                            letterSpacing = 0.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF450A0A)
                        ) {
                            Text("¡GANADOR!", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF5252), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF064E3B)
                        ) {
                            Text("Ventas", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                    }
                }

                // Grid 10x10
                for (row in 0..9) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..9) {
                            val number = String.format(Locale.US, "%02d", row * 10 + col)
                            val isWinner = winningNumbers.contains(number)
                            val soldQty = salesByNumber[number] ?: 0.0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .border(0.5.dp, PosBorder)
                                    .background(if (isWinner) Color(0xFF332B00) else Color.Transparent)
                                    .clickable {
                                        if (soldQty > 0.0) {
                                            val itemsForNumber = drawSummary.items.filter { it.number == number }
                                            val uniqueSaleIds = itemsForNumber.map { it.saleId }.distinct()
                                            val saleData = ticketMap[uniqueSaleIds.first()]
                                            if (saleData != null) {
                                                showTicketModal = saleData
                                            }
                                        }
                                    }
                                    .then(
                                        if (isWinner) Modifier.border(1.5.dp, Color(0xFFFFD700))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = number,
                                        fontSize = 12.sp,
                                        color = if (isWinner) Color(0xFFFFD700) else PosTextSecondary,
                                        fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Normal
                                    )
                                    if (soldQty > 0.0) {
                                        Text(
                                            text = "$soldQty",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E676)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // MODALS
    if (showTicketModal != null) {
        ReceiptDialog(
            saleWithItems = showTicketModal!!,
            resultsMap = resultsMap,
            onDismiss = { showTicketModal = null },
            onNewSale = { showTicketModal = null }
        )
    }

}


// ====================================================
// EMPTY STATE COMPONENT
// ====================================================
@Composable
fun EmptyArchiveBox(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = PosTextDisabled,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = message,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = PosTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ====================================================
// DATA AGGREGATION STRUCTURES AND FUNCTIONS
// ====================================================
data class YearArchiveData(
    val year: Int,
    val months: MutableMap<Int, MonthArchiveData> = mutableMapOf()
) {
    val totalSales: Double get() = months.values.sumOf { it.totalSales }
    val totalCommission: Double get() = months.values.sumOf { it.totalCommission }
    val totalPrizes: Double get() = months.values.sumOf { it.totalPrizes }
    val utility: Double get() = totalSales - totalCommission - totalPrizes
    val totalTicketsCount: Int get() = months.values.sumOf { it.totalTicketsCount }
    val totalDrawsCount: Int get() = months.values.sumOf { it.totalDrawsCount }
}

data class MonthArchiveData(
    val monthNumber: Int,
    val monthName: String,
    val days: MutableMap<String, DayArchiveData> = mutableMapOf()
) {
    val totalSales: Double get() = days.values.sumOf { it.totalSales }
    val totalCommission: Double get() = days.values.sumOf { it.totalCommission }
    val totalPrizes: Double get() = days.values.sumOf { it.totalPrizes }
    val utility: Double get() = totalSales - totalCommission - totalPrizes
    val totalTicketsCount: Int get() = days.values.sumOf { it.totalTicketsCount }
    val totalDrawsCount: Int get() = days.values.sumOf { it.totalDrawsCount }
}

data class DayArchiveData(
    val dateStr: String,
    val draws: MutableMap<String, DrawArchiveSummary> = mutableMapOf()
) {
    val totalSales: Double get() = draws.values.sumOf { it.sales }
    val totalCommission: Double get() = draws.values.sumOf { it.commission }
    val totalPrizes: Double get() = draws.values.sumOf { it.prizes }
    val utility: Double get() = totalSales - totalCommission - totalPrizes
    val totalTicketsCount: Int get() = draws.values.sumOf { it.ticketsCount }
    val totalDrawsCount: Int get() = draws.size
}

data class DrawArchiveSummary(
    val drawId: String,
    val drawName: String,
    val drawIcon: String,
    val drawTime: String,
    var sales: Double = 0.0,
    var commission: Double = 0.0,
    var prizes: Double = 0.0,
    var ticketsCount: Int = 0,
    val items: MutableList<SaleItem> = mutableListOf()
) {
    val utility: Double get() = sales - commission - prizes
}

private fun computeArchiveHierarchy(
    sales: List<SaleWithItems>,
    resultsMap: Map<String, DrawResult>
): Map<Int, YearArchiveData> {
    val yearMap = mutableMapOf<Int, YearArchiveData>()

    val monthNames = arrayOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val cal = Calendar.getInstance()

    for (saleWithItems in sales) {
        val sale = saleWithItems.sale
        cal.timeInMillis = sale.createdAt
        val year = cal.get(Calendar.YEAR)
        val monthNum = cal.get(Calendar.MONTH) + 1
        val monthName = monthNames[cal.get(Calendar.MONTH)]
        val dateStr = dateFormat.format(Date(sale.createdAt))

        val yearData = yearMap.getOrPut(year) { YearArchiveData(year) }
        val monthData = yearData.months.getOrPut(monthNum) { MonthArchiveData(monthNum, monthName) }
        val dayData = monthData.days.getOrPut(dateStr) { DayArchiveData(dateStr) }

        // Group items by draw
        val itemsByDraw = saleWithItems.items.groupBy { it.drawId }

        for ((drawId, items) in itemsByDraw) {
            val firstItem = items.firstOrNull() ?: continue
            val drawSummary = dayData.draws.getOrPut(drawId) {
                DrawArchiveSummary(
                    drawId = drawId,
                    drawName = firstItem.drawName,
                    drawIcon = firstItem.drawIcon,
                    drawTime = firstItem.drawTime
                )
            }

            val drawSales = items.sumOf { it.total }
            val drawCommission = drawSales * 0.15 // 15% commission standard
            val result = resultsMap[drawId]
            val drawPrizes = items.sumOf { item ->
                PrizeCalculator.calculateItemPrize(item, result)
            }

            drawSummary.sales += drawSales
            drawSummary.commission += drawCommission
            drawSummary.prizes += drawPrizes
            drawSummary.ticketsCount += 1
            drawSummary.items.addAll(items)
        }
    }

    return yearMap
}

private fun getStartOfDayCalendar(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun getEndOfDayCalendar(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}
