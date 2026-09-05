package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.SaleWithItems
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.Draw
import com.example.data.model.DrawResult
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.util.PrizeCalculator
import com.example.util.ThermalReceiptHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. TOP APP HEADER (METRICS STATUS BAR: VENTAS, COMISIÓN, BALANCE)
// ==========================================
@Composable
fun AppHeader(
    totalSales: Double = 0.0,
    totalCommission: Double = 0.0,
    netBalance: Double = 0.0,
    onMenuClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PosBackgroundSecondary,
        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Menu button
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú",
                    tint = PosTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Middle: Metrics (VENTAS | COMISIÓN | BALANCE NETO)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Ventas
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VENTAS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalSales)}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Divider 1
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(PosBorder)
                )

                // 2. Comisión
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COMISIÓN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalCommission)}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9800)
                    )
                }

                // Divider 2
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(PosBorder)
                )

                // 3. Balance Neto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BALANCE NETO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", netBalance)}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00E676)
                    )
                }
            }

            // Right: Logout / Exit button
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = PosTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. ACTIVE DRAW SELECTOR
// ==========================================
@Composable
fun ActiveDrawSelector(
    selectedDraw: Draw?,
    isMultiMode: Boolean,
    selectedMultiDrawsCount: Int,
    onOpenSelector: () -> Unit,
    onToggleMulti: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SORTEO ACTIVO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PosTextSecondary,
                letterSpacing = 0.5.sp
            )

            // Multi Mode Toggle Button
            Surface(
                onClick = onToggleMulti,
                shape = RoundedCornerShape(6.dp),
                color = if (isMultiMode) PosGreenPrimary else PosPanelSecondary,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMultiMode) PosGreenActive else PosBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isMultiMode) Icons.Default.Checklist else Icons.Outlined.Checklist,
                        contentDescription = "Multi",
                        tint = if (isMultiMode) PosBackground else PosTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isMultiMode) "MULTI ($selectedMultiDrawsCount)" else "MULTI-SORTEO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMultiMode) PosBackground else PosTextSecondary
                    )
                }
            }
        }

        // Main Draw Card (Click to open selector)
        Card(
            onClick = onOpenSelector,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PosPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
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
                    // Status pulse dot + icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PosPanelSecondary)
                            .border(1.dp, PosBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMultiMode) "🎯" else (selectedDraw?.icon?.ifBlank { "🎲" } ?: "🎲"),
                            fontSize = 20.sp
                        )
                    }

                    Column {
                        if (isMultiMode) {
                            Text(
                                text = "$selectedMultiDrawsCount sorteos seleccionados",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosGreenActive,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Jugadas registradas simultáneamente en lote",
                                fontSize = 12.sp,
                                color = PosTextSecondary
                            )
                        } else if (selectedDraw != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(PosGreenAction)
                                )
                                Text(
                                    text = "${selectedDraw.drawTime} — ${selectedDraw.name}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Cierre a las ${selectedDraw.closingTime}  •  Máx. ${selectedDraw.maxDigits()} cifras",
                                fontSize = 12.sp,
                                color = PosTextSecondary
                            )
                        } else {
                            Text(
                                text = "Seleccionar sorteo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PosTextSecondary
                            )
                            Text(
                                text = "Toque para elegir un sorteo activo",
                                fontSize = 11.sp,
                                color = PosTextDisabled
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Desplegar",
                    tint = PosGreenAction,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. GAME MODE SELECTOR (CHANCE | PALÉ)
// ==========================================
@Composable
fun GameModeSelector(
    selectedModality: String,
    allowedModalities: String,
    onSelectModality: (String) -> Unit
) {
    val modes = allowedModalities.split(",")
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }

    if (modes.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = PosBackgroundSecondary,
        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            modes.forEach { mode ->
                val isSelected = selectedModality == mode
                Surface(
                    onClick = { onSelectModality(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PosGreenPrimary else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) PosGreenActive else Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mode == "PALE") "PALÉ" else mode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) PosBackground else PosTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. NUMBER & QUANTITY DISPLAY WITH ACTIVE FOCUS (COMPACT)
// ==========================================
@Composable
fun NumberDisplayAndQuantity(
    number: String,
    modality: String,
    quantity: Double,
    quantityInput: String = "",
    unitPrice: Double,
    activeField: String, // "NUMBER" or "QUANTITY"
    onSelectField: (String) -> Unit,
    onIncrementQty: () -> Unit = {},
    onDecrementQty: () -> Unit = {},
    onClearNumber: () -> Unit
) {
    val total = quantity * unitPrice
    val playTitle = if (modality == "PALE") "PALÉ" else "CHANCE"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // NÚMERO / PLAY BOX (Clickable focus box)
            val isNumberActive = activeField == "NUMBER"
            Card(
                onClick = { onSelectField("NUMBER") },
                modifier = Modifier
                    .weight(1.15f)
                    .height(68.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNumberActive) PosPanelSecondary else PosPanel
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isNumberActive) 1.5.dp else 1.0.dp,
                    if (isNumberActive) PosGreenAction else PosBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isNumberActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PosGreenAction)
                                )
                            }
                            Text(
                                text = playTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNumberActive) PosGreenActive else PosTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (number.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = PosTextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onClearNumber() }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (number.isEmpty()) "--" else number,
                            fontSize = if (number.length > 4) 22.sp else 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (number.isEmpty()) PosTextDisabled else PosGreenActive,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // CANTIDAD BOX (Clickable focus box - compact without +/- buttons)
            val isQtyActive = activeField == "QUANTITY"
            Card(
                onClick = { onSelectField("QUANTITY") },
                modifier = Modifier
                    .weight(0.85f)
                    .height(68.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isQtyActive) PosPanelSecondary else PosPanel
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isQtyActive) 1.5.dp else 1.0.dp,
                    if (isQtyActive) PosGreenAction else PosBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isQtyActive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PosGreenAction)
                            )
                        }
                        Text(
                            text = "CANTIDAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isQtyActive) PosGreenActive else PosTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayQty = when {
                            isQtyActive && quantityInput.isEmpty() -> "1"
                            quantityInput.isNotEmpty() -> quantityInput
                            else -> if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                        }
                        Text(
                            text = displayQty,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isQtyActive && quantityInput.isEmpty()) PosTextDisabled else if (isQtyActive) PosGreenActive else PosTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. TOUCH NUMBER PAD (3x4 GRID WITH . AND ⌫)
// ==========================================
@Composable
fun NumberPad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onAddPlay: () -> Unit = {},
    onQuickAddQty: (Double) -> Unit = {},
    onMultiplyQty: (Double) -> Unit = {},
    onResetQty: () -> Unit = {}
) {
    var lastPadKey by remember { mutableStateOf("") }
    var lastPadKeyTime by remember { mutableStateOf(0L) }

    val keypad = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Keypad grid (3x4)
        keypad.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isDeleteKey = key == "⌫"
                    val isDotKey = key == "."

                    Surface(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (key == lastPadKey && now - lastPadKeyTime < 320L) {
                                return@Surface
                            }
                            if (now - lastPadKeyTime < 70L) {
                                return@Surface
                            }
                            lastPadKey = key
                            lastPadKeyTime = now

                            when (key) {
                                "⌫" -> onBackspace()
                                else -> onDigitClick(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isDeleteKey -> PosPanelSecondary
                            isDotKey -> PosPanelSecondary
                            else -> PosPanel
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDeleteKey) PosError.copy(alpha = 0.4f) else PosBorder
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDeleteKey) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Borrar",
                                    tint = PosError,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    fontSize = if (isDotKey) 26.sp else 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDotKey) PosGreenAction else PosTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. DRAW SELECTION DIALOG (CHRONOLOGICAL + STATUS)
// ==========================================
@Composable
fun DrawSelectionDialog(
    draws: List<Draw>,
    selectedDrawId: String?,
    onSelectDraw: (Draw) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredDraws = draws.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.drawTime.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .systemBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = PosBackgroundSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SELECCIONAR SORTEO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextPrimary
                        )
                        Text(
                            text = "Ordenados por horario de cierre",
                            fontSize = 12.sp,
                            color = PosTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PosTextSecondary)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar sorteo o por hora...", fontSize = 13.sp, color = PosTextDisabled) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PosTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PosPanel,
                        unfocusedContainerColor = PosPanel,
                        focusedBorderColor = PosGreenAction,
                        unfocusedBorderColor = PosBorder,
                        focusedTextColor = PosTextPrimary,
                        unfocusedTextColor = PosTextPrimary
                    )
                )

                // List of Draws
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDraws) { draw ->
                        val isSelected = draw.id == selectedDrawId

                        Surface(
                            onClick = {
                                onSelectDraw(draw)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PosPanelSecondary else PosPanel,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.0.dp,
                                if (isSelected) PosGreenAction else PosBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    
                                    Column {
                                        Text(
                                            text = draw.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosTextPrimary
                                        )
                                        Text(
                                            text = "Hora: ${draw.drawTime}  •  Cierre: ${draw.closingTime}",
                                            fontSize = 12.sp,
                                            color = PosTextSecondary
                                        )
                                        Text(
                                            text = "Modalidades: ${draw.allowedModalities}",
                                            fontSize = 10.sp,
                                            color = PosGreenActive
                                        )
                                    }
                                }

                                // Status Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (draw.active) PosGreenGlow else PosErrorLight,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (draw.active) PosGreenAction else PosError
                                    )
                                ) {
                                    Text(
                                        text = if (draw.active) "ACTIVO" else "CERRADO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (draw.active) PosGreenActive else PosError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

// ==========================================
// 7. MULTI DRAW SELECTION DIALOG
// ==========================================
@Composable
fun MultiDrawSelectionDialog(
    draws: List<Draw>,
    selectedIds: Set<String>,
    onToggleDraw: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .systemBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = PosBackgroundSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MODO MULTI-SORTEO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosGreenActive
                        )
                        Text(
                            text = "Selecciona sorteos para registrar jugadas en lote",
                            fontSize = 11.sp,
                            color = PosTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PosTextSecondary)
                    }
                }

                // Selected Counter
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = PosPanel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sorteos seleccionados:",
                            fontSize = 13.sp,
                            color = PosTextSecondary
                        )
                        Text(
                            text = "${selectedIds.size} de ${draws.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosGreenAction
                        )
                    }
                }

                // List of Draws with Checkboxes
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(draws) { draw ->
                        val isChecked = selectedIds.contains(draw.id)

                        Surface(
                            onClick = { onToggleDraw(draw.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChecked) PosPanelSecondary else PosPanel,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChecked) PosGreenAction else PosBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    
                                    Column {
                                        Text(
                                            text = draw.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosTextPrimary
                                        )
                                        Text(
                                            text = "${draw.drawTime}  •  Cierre: ${draw.closingTime}",
                                            fontSize = 11.sp,
                                            color = PosTextSecondary
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleDraw(draw.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PosGreenAction,
                                        uncheckedColor = PosTextDisabled,
                                        checkmarkColor = PosBackground
                                    )
                                )
                            }
                        }
                    }
                }

                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenPrimary)
                ) {
                    Text(
                        text = "APLICAR (${selectedIds.size} SORTEOS)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PosBackground
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. TICKET CART BOTTOM SHEET & DIALOG
// ==========================================
@Composable
fun EditCartItemDialog(
    item: CartItem,
    onConfirm: (newNumber: String, newQuantity: Double, newModality: String) -> Unit,
    onDismiss: () -> Unit
) {
    var editNumber by remember { mutableStateOf(item.number) }
    var editQuantityText by remember { mutableStateOf(item.quantity.toString()) }
    var editModality by remember { mutableStateOf(item.modality) }

    val allowedModalities = item.draw.allowedModalities.split(",").map { it.trim().uppercase() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .systemBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = PosBackgroundSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EDITAR JUGADA",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosGreenAction,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${item.draw.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PosTextSecondary)
                    }
                }

                // Modality Selector
                if (allowedModalities.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "MODALIDAD:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            allowedModalities.forEach { mod ->
                                val isSel = mod == editModality
                                Surface(
                                    onClick = {
                                        editModality = mod
                                        if (mod == "CHANCE" && editNumber.contains("-")) {
                                            editNumber = editNumber.replace("-", "").take(2)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) PosGreenPrimary else PosPanel,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) PosGreenActive else PosBorder
                                    )
                                ) {
                                    Text(
                                        text = mod,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) PosBackground else PosTextPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Number Input
                OutlinedTextField(
                    value = editNumber,
                    onValueChange = { editNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Número jugado") },
                    placeholder = { Text(if (editModality == "PALE") "Ej. 25-78" else "Ej. 25") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PosPanel,
                        unfocusedContainerColor = PosPanel,
                        focusedBorderColor = PosGreenAction,
                        unfocusedBorderColor = PosBorder,
                        focusedTextColor = PosTextPrimary,
                        unfocusedTextColor = PosTextPrimary
                    )
                )

                // Quantity Direct Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CANTIDAD:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary
                    )
                    OutlinedTextField(
                        value = editQuantityText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }.take(4)
                            editQuantityText = digitsOnly
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ingresar cantidad") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PosPanel,
                            unfocusedContainerColor = PosPanel,
                            focusedBorderColor = PosGreenAction,
                            unfocusedBorderColor = PosBorder,
                            focusedTextColor = PosTextPrimary,
                            unfocusedTextColor = PosTextPrimary
                        )
                    )

                    // Quick presets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 5, 10, 20, 50, 100).forEach { preset ->
                            Surface(
                                onClick = { editQuantityText = preset.toString() },
                                shape = RoundedCornerShape(6.dp),
                                color = PosPanel,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$preset",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PosTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val cleanNum = editNumber.trim()
                            val parsedQty = editQuantityText.toDoubleOrNull() ?: 1.0
                            val finalQty = if (parsedQty in 0.01..9999.0) parsedQty else 1.0
                            if (cleanNum.isNotEmpty()) {
                                onConfirm(cleanNum, finalQty, editModality)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                    ) {
                        Text(
                            text = "Guardar",
                            fontWeight = FontWeight.Bold,
                            color = PosBackground
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inline Cart Section displayed directly below the sales keypad on SalesScreen.
 */
@Composable
fun TicketCartInlineSection(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Double) -> Unit = { _, _ -> },
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onProceedToCustomer: () -> Unit,
    isEditing: Boolean = false,
    editingTicketNumber: String? = null,
    onCancelEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (cartItems.isEmpty()) return

    val totalAmount = cartItems.sumOf { it.total }
    val totalPlays = cartItems.sumOf { it.quantity }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PosBackgroundSecondary),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isEditing) Color(0xFFF59E0B) else PosBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cart Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isEditing) Color(0xFFF59E0B) else PosGreenAction)
                    )
                    Text(
                        text = if (isEditing) "EDITANDO TICKET ${editingTicketNumber ?: ""} (${cartItems.size})" else "CARRITO / TICKET EN CURSO (${cartItems.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isEditing) Color(0xFFF59E0B) else PosGreenActive,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$totalPlays jugada(s)",
                        fontSize = 11.sp,
                        color = PosTextSecondary
                    )

                    if (isEditing && onCancelEdit != null) {
                        TextButton(
                            onClick = onCancelEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar edición",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Cancelar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onClearCart,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar",
                                tint = PosError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vaciar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosError
                            )
                        }
                    }
                }
            }

            Divider(color = PosBorder, thickness = 1.dp)

            // Group plays by draw
            val groupedCart = remember(cartItems) {
                cartItems.groupBy { it.draw.id }
            }

            // Items List Grouped by Sorteo
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedCart.entries.forEachIndexed { groupIndex, (_, drawItems) ->
                    val firstItem = drawItems.first()
                    val draw = firstItem.draw
                    val drawTotal = drawItems.sumOf { it.total }
                    val drawPlaysCount = drawItems.sumOf { it.quantity }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Sorteo Header Tag
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = PosPanelSecondary,
                            border = BorderStroke(1.dp, PosGreenActive.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${draw.name.uppercase()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PosGreenActive,
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PosPanel,
                                        border = BorderStroke(0.5.dp, PosBorder)
                                    ) {
                                        Text(
                                            text = "$drawPlaysCount pz${if (drawPlaysCount > 1) "s" else ""}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosTextSecondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", drawTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosGreenAction,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Plays for this Draw
                        drawItems.forEach { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = PosPanel,
                                border = BorderStroke(1.dp, PosBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Number badge and modality
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Monospace Highlighted Number
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PosPanelSecondary,
                                            border = BorderStroke(1.dp, PosGreenActive.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = item.number,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PosGreenActive,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = item.modality,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PosTextPrimary
                                            )
                                            Text(
                                                text = "c/u $${String.format(Locale.US, "%.2f", item.unitPrice)}",
                                                fontSize = 10.sp,
                                                color = PosTextDisabled
                                            )
                                        }
                                    }

                                    // Middle: Direct Editable Quantity Input
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Cant:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosTextSecondary
                                        )
                                        var localQtyText by remember(item.id, item.quantity) {
                                            mutableStateOf(item.quantity.toString())
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PosPanelSecondary,
                                            border = BorderStroke(1.dp, PosGreenActive.copy(alpha = 0.6f))
                                        ) {
                                            BasicTextField(
                                                value = localQtyText,
                                                onValueChange = { input ->
                                                    val digits = input.filter { it.isDigit() }.take(4)
                                                    localQtyText = digits
                                                    val parsed = digits.toDoubleOrNull()
                                                    if (parsed != null && parsed in 0.01..9999.0) {
                                                        onUpdateQuantity(item.id, parsed)
                                                    }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = TextStyle(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = PosGreenActive,
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.Center
                                                ),
                                                cursorBrush = SolidColor(PosGreenActive),
                                                modifier = Modifier
                                                    .width(46.dp)
                                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Right: Subtotal & Delete Button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", item.total)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PosGreenAction
                                        )

                                        IconButton(
                                            onClick = { onRemoveItem(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar jugada",
                                                tint = PosError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (groupIndex < groupedCart.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.8.dp,
                            color = PosGreenActive.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            // Subtotal and Grand Total Breakdown
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = PosPanelSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL TICKET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${cartItems.size} ítems / $totalPlays jugadas",
                            fontSize = 10.sp,
                            color = PosTextDisabled
                        )
                    }

                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalAmount)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosGreenAction,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Prominent Emit Ticket Action Button / Confirm Edit Button
            Button(
                onClick = onProceedToCustomer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditing) PosGreenAction else PosGreenAction
                )
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.CheckCircle else Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = PosBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) {
                        "CONFIRMAR VENTA EDITADA ($${String.format(Locale.US, "%.2f", totalAmount)})"
                    } else {
                        "CONFIRMAR Y EMITIR TICKET ($${String.format(Locale.US, "%.2f", totalAmount)})"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PosBackground,
                    letterSpacing = 0.5.sp
                )
            }

            if (isEditing && onCancelEdit != null) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CANCELAR EDICIÓN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketCartBottomSheet(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Double) -> Unit = { _, _ -> },
    onEditItem: (CartItem) -> Unit = {},
    onRemoveItem: (String) -> Unit,
    onIncrementQty: (String) -> Unit = {},
    onDecrementQty: (String) -> Unit = {},
    onClearCart: () -> Unit,
    onProceedToCustomer: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalAmount = cartItems.sumOf { it.total }
    val commission = totalAmount * 0.05

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PosBackgroundSecondary,
        contentColor = PosTextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(PosBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = PosGreenAction,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "TICKET ACTUAL (${cartItems.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextPrimary
                    )
                }

                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = onClearCart) {
                        Text(
                            text = "VACIAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosError
                        )
                    }
                }
            }

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = PosTextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "El ticket está vacío",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PosTextSecondary
                        )
                        Text(
                            text = "Ingresa números y agrégalos para facturar",
                            fontSize = 12.sp,
                            color = PosTextDisabled
                        )
                    }
                }
            } else {
                val groupedSheetCart = remember(cartItems) {
                    cartItems.groupBy { it.draw.id }
                }

                // List of plays grouped by draw
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedSheetCart.entries.forEachIndexed { groupIndex, (_, drawItems) ->
                        val firstItem = drawItems.first()
                        val draw = firstItem.draw
                        val drawTotal = drawItems.sumOf { it.total }
                        val drawPlaysCount = drawItems.sumOf { it.quantity }

                        item(key = "header_${draw.id}") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = PosPanelSecondary,
                                border = BorderStroke(1.dp, PosGreenActive.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${draw.name.uppercase()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PosGreenActive,
                                            letterSpacing = 0.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PosPanel,
                                            border = BorderStroke(0.5.dp, PosBorder)
                                        ) {
                                            Text(
                                                text = "$drawPlaysCount pz${if (drawPlaysCount > 1) "s" else ""}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PosTextSecondary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", drawTotal)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosGreenAction,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        items(drawItems, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = PosPanel,
                                border = BorderStroke(1.dp, PosBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Number Pill
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PosPanelSecondary,
                                            border = BorderStroke(1.dp, PosGreenActive.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = item.number,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PosGreenActive,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = item.modality,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PosTextPrimary
                                            )
                                            Text(
                                                text = "c/u $${String.format(Locale.US, "%.2f", item.unitPrice)}",
                                                fontSize = 10.sp,
                                                color = PosTextDisabled
                                            )
                                        }
                                    }

                                    // Middle: Direct Editable Quantity Input
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Cant:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosTextSecondary
                                        )
                                        var localQtySheet by remember(item.id, item.quantity) {
                                            mutableStateOf(item.quantity.toString())
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PosPanelSecondary,
                                            border = BorderStroke(1.dp, PosGreenActive.copy(alpha = 0.6f))
                                        ) {
                                            BasicTextField(
                                                value = localQtySheet,
                                                onValueChange = { input ->
                                                    val digits = input.filter { it.isDigit() }.take(4)
                                                    localQtySheet = digits
                                                    val parsed = digits.toDoubleOrNull()
                                                    if (parsed != null && parsed in 0.01..9999.0) {
                                                        onUpdateQuantity(item.id, parsed)
                                                    }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = TextStyle(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = PosGreenActive,
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.Center
                                                ),
                                                cursorBrush = SolidColor(PosGreenActive),
                                                modifier = Modifier
                                                    .width(46.dp)
                                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", item.total)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosGreenAction
                                        )
                                        IconButton(
                                            onClick = { onRemoveItem(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = PosError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (groupIndex < groupedSheetCart.size - 1) {
                            item(key = "divider_${draw.id}") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 0.8.dp,
                                    color = PosGreenActive.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }

                // Totals Breakdown
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = PosPanel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", fontSize = 12.sp, color = PosTextSecondary)
                            Text("$${String.format(Locale.US, "%.2f", totalAmount)}", fontSize = 12.sp, color = PosTextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Comisión estimada (5%):", fontSize = 12.sp, color = PosTextSecondary)
                            Text("$${String.format(Locale.US, "%.2f", commission)}", fontSize = 12.sp, color = PosGreenActive)
                        }
                        Divider(color = PosBorder, thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL A COBRAR:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PosTextPrimary)
                            Text(
                                "$${String.format(Locale.US, "%.2f", totalAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PosGreenAction
                            )
                        }
                    }
                }

                // Action: Confirm Sale
                Button(
                    onClick = onProceedToCustomer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PosBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIRMAR VENTA ($${String.format(Locale.US, "%.2f", totalAmount)})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosBackground
                    )
                }
            }
        }
    }
}

// ==========================================
// 9. CUSTOMER INFORMATION MODAL
// ==========================================
@Composable
fun CustomerModal(
    savedCustomers: List<Customer>,
    initialCustomerName: String = "",
    isEditing: Boolean = false,
    ticketNumber: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customerName by remember(initialCustomerName) {
        mutableStateOf(initialCustomerName)
    }
    var searchQuery by remember { mutableStateOf("") }

    val recentCustomers = savedCustomers.take(5)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .systemBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = PosBackgroundSecondary,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isEditing) Color(0xFFF59E0B) else PosBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "CONFIRMAR VENTA EDITADA" else "INFORMACIÓN DE VENTA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEditing) Color(0xFFF59E0B) else PosTextPrimary
                        )
                        Text(
                            text = if (isEditing) {
                                "Ticket ${ticketNumber ?: ""} • Revisa el cliente y confirma los cambios"
                            } else {
                                "Ingresa el nombre del cliente para el ticket"
                            },
                            fontSize = 11.sp,
                            color = PosTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PosTextSecondary)
                    }
                }

                // Name Input field
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del cliente *") },
                    placeholder = { Text("Ej. Juan Pérez, Mostrador...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PosPanel,
                        unfocusedContainerColor = PosPanel,
                        focusedBorderColor = if (isEditing) Color(0xFFF59E0B) else PosGreenAction,
                        unfocusedBorderColor = PosBorder,
                        focusedTextColor = PosTextPrimary,
                        unfocusedTextColor = PosTextPrimary,
                        focusedLabelColor = if (isEditing) Color(0xFFF59E0B) else PosGreenActive,
                        unfocusedLabelColor = PosTextSecondary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isEditing) Color(0xFFF59E0B) else PosGreenAction
                        )
                    }
                )

                // Quick selector for recent/frequent customers
                if (recentCustomers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CLIENTES FRECUENTES / RÁPIDOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(recentCustomers) { cust ->
                                Surface(
                                    onClick = { customerName = cust.name },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PosPanel,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                                ) {
                                    Text(
                                        text = cust.name,
                                        fontSize = 12.sp,
                                        color = PosTextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            item {
                                Surface(
                                    onClick = { customerName = "Cliente Mostrador" },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PosPanelSecondary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                                ) {
                                    Text(
                                        text = "Mostrador",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosGreenActive,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Confirm button
                Button(
                    onClick = {
                        val finalName = customerName.ifBlank { "Cliente Mostrador" }
                        onConfirm(finalName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PosGreenAction
                    )
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        tint = PosBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditing) "CONFIRMAR VENTA EDITADA" else "FINALIZAR VENTA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosBackground
                    )
                }
            }
        }
    }
}

// ==========================================
// 10. THERMAL RECEIPT DIALOG (58MM COMPACT WHITE/GREEN THEME)
// ==========================================

@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2E7D32).copy(alpha = 0.5f),
    dashWidth: Float = 8f,
    gapWidth: Float = 5f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f)
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = pathEffect
        )
    }
}

@Composable
fun ReceiptQRCode(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 100
) {
    val matrixSize = 21
    val isDark = remember(data) {
        val matrix = Array(matrixSize) { BooleanArray(matrixSize) }
        val hash = (data.hashCode().toLong() and 0xFFFFFFFFL)

        // Draw Finder Patterns (top-left, top-right, bottom-left 7x7)
        fun drawFinder(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startY + r][startX + c] = isBorder || isCenter
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(matrixSize - 7, 0)
        drawFinder(0, matrixSize - 7)

        // Timing patterns
        for (i in 7 until matrixSize - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Fill remaining data bits deterministically
        var bitIndex = 0
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                val inFinder = (r < 8 && c < 8) || (r < 8 && c >= matrixSize - 8) || (r >= matrixSize - 8 && c < 8)
                val inTiming = (r == 6 || c == 6)
                if (!inFinder && !inTiming) {
                    val bit1 = ((hash shr (bitIndex % 31)) and 1L) == 1L
                    val bit2 = ((r * 13 + c * 7 + bitIndex) % 3) == 0
                    val pseudoRandomBit = bit1 xor bit2
                    matrix[r][c] = pseudoRandomBit
                    bitIndex++
                }
            }
        }
        matrix
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(6.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / matrixSize
            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (isDark[r][c]) {
                        drawRect(
                            color = Color(0xFF1B5E20),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    saleWithItems: SaleWithItems,
    resultsMap: Map<String, DrawResult> = emptyMap(),
    onDismiss: () -> Unit,
    onNewSale: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sale = saleWithItems.sale
    val items = saleWithItems.items

    var showBtDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(sale.createdAt))
    val formattedTime = timeFormat.format(Date(sale.createdAt))

    // Formatted strictly for 32 columns standard 58mm thermal paper with draw separation & results
    val receiptText = remember(saleWithItems, resultsMap) {
        ThermalReceiptHelper.formatTicket58mm(saleWithItems, resultsMap)
    }

    // Group items by draw for independent visualization and calculation
    val groupedByDraw = remember(items) {
        items.groupBy { it.drawId }
    }

    // Calculate overall ticket prizes
    val totalTicketPrizes = remember(saleWithItems, resultsMap) {
        if (sale.status == "ANULADA") 0.0 else {
            saleWithItems.items.sumOf { play ->
                val res = resultsMap[play.drawId]
                if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
            }
        }
    }

    if (showBtDialog) {
        val bluetoothAdapter = remember {
            try { android.bluetooth.BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        }
        val pairedDevices = remember(showBtDialog) {
            try {
                bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        AlertDialog(
            onDismissRequest = { showBtDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = PosGreenAction)
                    Text("Impresora Bluetooth 58mm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pairedDevices.isEmpty()) {
                        Text(
                            "No se encontraron impresoras Bluetooth vinculadas.\n\nPor favor empareje su impresora térmica portátil de 58mm en los Ajustes de Bluetooth de Android antes de imprimir.",
                            fontSize = 13.sp,
                            color = PosTextSecondary
                        )
                    } else {
                        Text("Seleccione la impresora para emitir el ticket:", fontSize = 13.sp, color = PosTextSecondary)
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            items(pairedDevices) { device ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            showBtDialog = false
                                            coroutineScope.launch {
                                                val res = ThermalReceiptHelper.printToBluetoothPrinter(context, device, saleWithItems)
                                                android.widget.Toast.makeText(context, res.second, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PosPanel,
                                    border = BorderStroke(1.dp, PosBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = PosGreenActive)
                                        Column {
                                            Text(device.name ?: "Dispositivo Bluetooth", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PosTextPrimary)
                                            Text(device.address, fontSize = 11.sp, color = PosTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBtDialog = false }) {
                    Text("Cerrar", color = PosGreenAction, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = PosBackgroundSecondary,
                border = BorderStroke(1.dp, PosBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                // Modal Top Bar
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PosGreenAction),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PosBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "COMPROBANTE DE VENTA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PosGreenActive
                            )
                            Text(
                                text = "Ticket: #${sale.ticketNumber} • ${items.size} jugada(s)",
                                fontSize = 11.sp,
                                color = PosTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PosPanel)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = PosTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ==========================================
                // WHITE TICKET (CLEAN THERMAL RECEIPT DESIGN)
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFFFF),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Ticket Header: Left Titles & Dates, Right QR Code
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "LOTERIA",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF15803D),
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "COMPROBANTE",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        letterSpacing = 0.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FECHA:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = formattedDate,
                                            fontSize = 11.sp,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "HORA: ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = formattedTime,
                                            fontSize = 11.sp,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                ReceiptQRCode(
                                    data = "TICKET:${sale.ticketNumber}|TOTAL:${sale.total}|DATE:${sale.createdAt}",
                                    sizeDp = 70
                                )
                            }
                        }

                        // Ticket Anulado Banner
                        if (sale.status == "ANULADA") {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, Color(0xFFE53935))
                                ) {
                                    Text(
                                        text = "*** TICKET ANULADO ***",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFC62828),
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Winner Banner (if winning ticket)
                        if (totalTicketPrizes > 0.0) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(2.dp, Color(0xFFF59E0B))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "¡ GANADOR! ¡¡¡",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB45309),
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "TOTAL: $${String.format(Locale.US, "%.2f", totalTicketPrizes)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF92400E),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Ticket Metadata: CLIENTE, VENDEDOR
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CLIENTE:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = sale.customerName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "VENDEDOR:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = sale.userName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // ==========================================
                        // SEPARATED DRAWS SECTIONS (MULTIPLE DRAWS ISOLATED)
                        // ==========================================
                        var drawIndex = 0
                        groupedByDraw.forEach { (drawId, drawItems) ->
                            val currentDrawIndex = drawIndex
                            val firstItem = drawItems.first()
                            val drawName = firstItem.drawName
                            val drawSubtotal = drawItems.sumOf { it.total }
                            val drawTxCode = ThermalReceiptHelper.getDrawTxCode(sale.id, drawId, currentDrawIndex)
                            val drawResult = resultsMap[drawId]
                            val drawPrizes = if (sale.status == "ANULADA") 0.0 else if (drawResult != null) {
                                drawItems.sumOf { play -> PrizeCalculator.calculateItemPrize(play, drawResult) }
                            } else 0.0

                            item(key = "draw_section_$drawId") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Sorteo Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📈 🇭🇳 $drawName 🌙",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF15803D),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Draw Results in 3 distinct columns: 1er Premio | 2do Premio | 3er Premio
                                    if (drawResult != null && (drawResult.firstPrize.isNotBlank() || drawResult.secondPrize.isNotBlank() || drawResult.thirdPrize.isNotBlank())) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Column 1: 1er Premio
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("1er Premio", fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    Text(
                                                        drawResult.firstPrize.ifBlank { "--" },
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFD97706),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                // Column 2: 2do Premio
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("2do Premio", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    Text(
                                                        drawResult.secondPrize.ifBlank { "--" },
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF334155),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                // Column 3: 3er Premio
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("3er Premio", fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    Text(
                                                        drawResult.thirdPrize.ifBlank { "--" },
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFD97706),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Table Column Headers
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "NÚMERO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Text(
                                            text = "TIPO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                        Text(
                                            text = "PZS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                        Text(
                                            text = "MONTO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1.1f)
                                        )
                                    }

                                    // List of plays for this specific draw
                                    drawItems.forEach { play ->
                                        val playPrize = if (sale.status == "ANULADA") 0.0 else if (drawResult != null) PrizeCalculator.calculateItemPrize(play, drawResult) else 0.0
                                        val isPlayWon = playPrize > 0.0
                                        val displayModality = if (play.modality.uppercase().startsWith("CH")) "CH" else if (play.modality.uppercase().startsWith("P")) "PL" else play.modality

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isPlayWon) {
                                                        Modifier
                                                            .background(Color(0xFFFEF9C3), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    } else {
                                                        Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    }
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = play.number + if (isPlayWon) " •" else "",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isPlayWon) Color(0xFFB45309) else Color(0xFF0F172A),
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(1.2f)
                                                )
                                                Text(
                                                    text = displayModality,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569),
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(0.9f)
                                                )
                                                Text(
                                                    text = "x${play.quantity}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569),
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(0.8f)
                                                )
                                                Text(
                                                    text = "$${String.format(Locale.US, "%.2f", play.total)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF0F172A),
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.weight(1.1f)
                                                )
                                            }

                                            // Prize breakdown for winning play
                                            if (isPlayWon) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 1.dp),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "+$${String.format(Locale.US, "%.2f", playPrize)}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFD97706),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Subtotal & TX for Draw
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TX: $drawTxCode",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "SUBTOTAL: $${String.format(Locale.US, "%.2f", drawSubtotal)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    DottedDivider()
                                }
                            }
                            drawIndex++
                        }

                        // Total Row & Disclaimer & Footer
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Total Sale Box (Green Box)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF15803D)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TOTAL :",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", sale.total)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Disclaimer Box with requested phrase
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ IMPORTANTE: Sin comprobante no se pagan premios.",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Por favor verificar su ticket, no se aceptan cambios luego del cierre.",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF475569),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Text(
                                    text = "¡ G R A C I A S   P O R   S U   C O M P R A !",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // ACTION BUTTONS BELOW TICKET
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: [ BT PRINT ] & [ IMPRIMIR ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showBtDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosPanel),
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = PosGreenActive, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BT PRINT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                ThermalReceiptHelper.printTicket58mm(context, saleWithItems, resultsMap)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosPanel),
                            border = BorderStroke(1.dp, PosBorder)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = PosGreenActive, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "IMPRIMIR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextPrimary
                            )
                        }
                    }

                    // Row 2: [ GUARDAR FOTO ]
                    Button(
                        onClick = {
                            ThermalReceiptHelper.saveReceiptAsImage(context, saleWithItems, resultsMap)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosPanel),
                        border = BorderStroke(1.dp, PosBorder)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = PosGreenActive, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GUARDAR FOTO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTextPrimary
                        )
                    }

                    // Row 3: [ COMPARTIR WHATSAPP ]
                    Button(
                        onClick = {
                            ThermalReceiptHelper.shareTicketStrictlyToWhatsApp(context, saleWithItems, resultsMap)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPARTIR WHATSAPP",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Bottom: CERRAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CERRAR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
// 11. LOGIN / ACCESO CONFIDENCIAL SCREEN
// ==========================================
@Composable
fun LoginScreen(
    users: List<User>,
    onLoginSuccess: (User) -> Unit
) {
    var selectedUser by remember(users) {
        mutableStateOf(users.firstOrNull() ?: User(name = "Admin Sistema", username = "admin", role = "ADMINISTRADOR"))
    }
    var password by remember { mutableStateOf("123456") }
    var showPassword by remember { mutableStateOf(false) }
    val loginScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PosBackground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(loginScrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PosBackgroundSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Security Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PosPanel)
                        .border(1.dp, PosGreenAction, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Seguridad",
                        tint = PosGreenAction,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACCESO CONFIDENCIAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosGreenAction,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Sistema de Gestión de Sorteos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextPrimary
                    )
                    Text(
                        text = "Ingresa tus credenciales para operar el terminal POS",
                        fontSize = 11.sp,
                        color = PosTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Divider(color = PosBorder, thickness = 1.dp)

                // Quick User Selector Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SELECCIONAR USUARIO:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users) { user ->
                            val isSelected = user.id == selectedUser.id
                            Surface(
                                onClick = { selectedUser = user },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PosGreenPrimary else PosPanel,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) PosGreenActive else PosBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = user.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PosBackground else PosTextPrimary
                                    )
                                    Text(
                                        text = "(${user.role.take(3)})",
                                        fontSize = 10.sp,
                                        color = if (isSelected) PosBackground.copy(alpha = 0.8f) else PosTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = PosTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PosPanel,
                        unfocusedContainerColor = PosPanel,
                        focusedBorderColor = PosGreenAction,
                        unfocusedBorderColor = PosBorder,
                        focusedTextColor = PosTextPrimary,
                        unfocusedTextColor = PosTextPrimary,
                        focusedLabelColor = PosGreenActive,
                        unfocusedLabelColor = PosTextSecondary
                    )
                )

                // Login Button
                Button(
                    onClick = { onLoginSuccess(selectedUser) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = PosBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INGRESAR AL SISTEMA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosBackground,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
