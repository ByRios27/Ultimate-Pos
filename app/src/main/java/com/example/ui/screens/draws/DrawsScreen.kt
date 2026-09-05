@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.draws

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Draw
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.util.UUID

@Composable
fun DrawsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val draws by viewModel.draws.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser.role == "ADMINISTRADOR"

    var showCreateModal by remember { mutableStateOf(false) }
    var drawToEdit by remember { mutableStateOf<Draw?>(null) }
    var drawToDuplicate by remember { mutableStateOf<Draw?>(null) }
    var drawToDelete by remember { mutableStateOf<Draw?>(null) }

    var filterActiveOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedTab by remember { mutableIntStateOf(0) }
    val filteredDraws = draws.filter { draw ->
        val matchesSearch = draw.name.contains(searchQuery, ignoreCase = true) ||
                draw.drawTime.contains(searchQuery, ignoreCase = true)
        val matchesActive = !filterActiveOnly || draw.active
        matchesSearch && matchesActive
    }

    Scaffold(
        floatingActionButton = {
            if (isAdmin && selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateModal = true },
                    containerColor = PosGreenAction,
                    contentColor = PosBackground,
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = PosBackground) },
                    text = { Text("+ NUEVO SORTEO", fontWeight = FontWeight.ExtraBold, color = PosBackground) }
                )
            }
        },
        containerColor = PosBackground
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PosPanel,
                contentColor = PosTextPrimary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PosGreenAction
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sorteos", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pagos y Precios", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GESTIÓN DE SORTEOS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Catálogo de Loterías",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PosTextPrimary
                                )
                            }
        
                            FilterChip(
                                selected = filterActiveOnly,
                                onClick = { filterActiveOnly = !filterActiveOnly },
                                label = { Text("Solo activos", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosGreenAction,
                                    selectedLabelColor = PosBackground,
                                    containerColor = PosPanel,
                                    labelColor = PosTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = filterActiveOnly,
                                    borderColor = if (filterActiveOnly) PosGreenAction else PosBorder
                                )
                            )
                        }
                    }
        
                    // Search box
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar sorteo por nombre o por hora...", fontSize = 13.sp, color = PosTextDisabled) },
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
                    }
        
                    // List of Sorteos
                    items(filteredDraws) { draw ->
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
                        // Title & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        text = "Hora Sorteo: ${draw.drawTime}  •  Cierre: ${draw.closingTime}",
                                        fontSize = 12.sp,
                                        color = PosTextSecondary
                                    )
                                }
                            }

                            StatusBadge(active = draw.active)
                        }

                        // Info details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PosPanelSecondary,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                            ) {
                                Text(
                                    text = "Modalidades: ${draw.allowedModalities}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PosGreenActive,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PosPanelSecondary,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                            ) {
                                Text(
                                    text = "Días: ${draw.recurrenceDays}",
                                    fontSize = 10.sp,
                                    color = PosTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Action Buttons (Toggle, Duplicate, Edit, Delete)
                        if (isAdmin) {
                            Divider(color = PosBorder, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Switch(
                                        checked = draw.active,
                                        onCheckedChange = { viewModel.toggleDrawActive(draw) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PosBackground,
                                            checkedTrackColor = PosGreenAction,
                                            uncheckedThumbColor = PosTextDisabled,
                                            uncheckedTrackColor = PosPanelSecondary
                                        )
                                    )
                                    Text(
                                        text = if (draw.active) "Habilitado" else "Inhabilitado",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (draw.active) PosGreenActive else PosTextDisabled
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { drawToDuplicate = draw },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = PosTextSecondary, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { drawToEdit = draw },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PosGreenAction, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { drawToDelete = draw },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = PosError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
        } else if (selectedTab == 1) {
            val config by viewModel.payoutConfig.collectAsState()
            PayoutConfigEditor(config = config, onSave = { viewModel.updatePayoutConfig(it) })
        }
    }
    }

    // Modal Create / Edit
    if (showCreateModal || drawToEdit != null || drawToDuplicate != null) {
        val isNew = drawToEdit == null
        val initialDraw = drawToEdit ?: drawToDuplicate?.copy(
            id = UUID.randomUUID().toString(),
            name = "${drawToDuplicate?.name} (Copia)"
        )

        DrawFormDialog(
            initialDraw = initialDraw,
            onSave = { draw ->
                viewModel.saveDraw(draw, isNew = isNew)
                showCreateModal = false
                drawToEdit = null
                drawToDuplicate = null
            },
            onDismiss = {
                showCreateModal = false
                drawToEdit = null
                drawToDuplicate = null
            }
        )
    }

    // Delete confirmation
    if (drawToDelete != null) {
        AlertDialog(
            onDismissRequest = { drawToDelete = null },
            containerColor = PosBackgroundSecondary,
            titleContentColor = PosTextPrimary,
            textContentColor = PosTextSecondary,
            title = { Text("Eliminar Sorteo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de eliminar '${drawToDelete?.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        drawToDelete?.let { viewModel.deleteDraw(it) }
                        drawToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosError)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { drawToDelete = null }) {
                    Text("Cancelar", color = PosTextSecondary)
                }
            }
        )
    }
}

@Composable
fun DrawFormDialog(
    initialDraw: Draw?,
    onSave: (Draw) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialDraw?.name ?: "") }
    var drawTime by remember { mutableStateOf(initialDraw?.drawTime ?: "12:00 PM") }
    var closingTime by remember { mutableStateOf(initialDraw?.closingTime ?: "11:50 AM") }
    var allowedDigits by remember { mutableStateOf(initialDraw?.allowedDigits ?: "1,2,3,4") }
    var allowedModalities by remember { mutableStateOf(initialDraw?.allowedModalities ?: "CHANCE,PALE") }
    var active by remember { mutableStateOf(initialDraw?.active ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = PosBackgroundSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialDraw == null) "NUEVO SORTEO" else "EDITAR SORTEO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Sorteo") },
                    placeholder = { Text("Ej. Anguila 10:00 AM, La Primera...") },
                    modifier = Modifier.fillMaxWidth(),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drawTime,
                        onValueChange = { drawTime = it },
                        label = { Text("Hora Sorteo") },
                        modifier = Modifier.weight(1f),
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

                    OutlinedTextField(
                        value = closingTime,
                        onValueChange = { closingTime = it },
                        label = { Text("Hora Cierre") },
                        modifier = Modifier.weight(1f),
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
                }

                var expandedDigits by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expandedDigits,
                    onExpandedChange = { expandedDigits = !expandedDigits },
                    modifier = Modifier.fillMaxWidth()
                ) {

                        OutlinedTextField(

                            value = allowedDigits.split(",").maxOrNull()?.plus(" cifras") ?: "4 cifras",

                            onValueChange = {},

                            readOnly = true,

                            label = { Text("Cifras") },

                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDigits) },

                            modifier = Modifier.fillMaxWidth().menuAnchor(),

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

                        ExposedDropdownMenu(

                            expanded = expandedDigits,

                            onDismissRequest = { expandedDigits = false }

                        ) {

                            (2..6).forEach { num ->

                                DropdownMenuItem(

                                    text = { Text("$num cifras") },

                                    onClick = {

                                        allowedDigits = (1..num).joinToString(",")

                                        expandedDigits = false

                                    }

                                )

                            }

                        }

                    }
                }

                OutlinedTextField(
                    value = allowedModalities,
                    onValueChange = { allowedModalities = it },
                    label = { Text("Modalidades (separadas por coma)") },
                    placeholder = { Text("CHANCE,PALE") },
                    modifier = Modifier.fillMaxWidth(),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sorteo Habilitado:", fontSize = 13.sp, color = PosTextPrimary)
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PosBackground,
                            checkedTrackColor = PosGreenAction,
                            uncheckedThumbColor = PosTextDisabled,
                            uncheckedTrackColor = PosPanel
                        )
                    )
                }

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
                            val draw = (initialDraw ?: Draw(name = name)).copy(
                                name = name.ifBlank { "Sorteo Sin Nombre" },
                                drawTime = drawTime,
                                closingTime = closingTime,
                                icon = "", // Leave blank to allow users to add emojis in the name
                                allowedDigits = allowedDigits.ifBlank { "1,2,3,4" },
                                allowedModalities = allowedModalities.uppercase(),
                                active = active
                            )
                            onSave(draw)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                    ) {
                        Text("Guardar", color = PosBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
