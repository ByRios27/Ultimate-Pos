package com.example.ui.screens.other

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.Customer
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

// ----------------------------------------------------
// CUSTOMERS SCREEN
// ----------------------------------------------------
@Composable
fun CustomersScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.alias.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PosGreenAction,
                contentColor = PosBackground,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PosBackground) },
                text = { Text("+ NUEVO CLIENTE", fontWeight = FontWeight.Bold, color = PosBackground) }
            )
        },
        containerColor = PosBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "DIRECTORIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Gestión de Clientes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextPrimary
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar cliente por nombre, alias o teléfono...", fontSize = 13.sp, color = PosTextDisabled) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PosTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PosPanel,
                        unfocusedContainerColor = PosPanel,
                        focusedBorderColor = PosGreenAction,
                        unfocusedBorderColor = PosBorder,
                        focusedTextColor = PosTextPrimary,
                        unfocusedTextColor = PosTextPrimary
                    ),
                    singleLine = true
                )
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PosPanel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron clientes registrados", color = PosTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            items(filteredCustomers) { cust ->
                val customerSales = sales.filter { it.sale.customerName.equals(cust.name, ignoreCase = true) && it.sale.status == "ACTIVA" }
                val totalPurchases = customerSales.sumOf { it.sale.total }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PosPanelSecondary)
                                    .border(1.dp, PosBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cust.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = PosGreenActive,
                                    fontSize = 15.sp
                                )
                            }

                            Column {
                                Text(text = cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PosTextPrimary)
                                if (cust.alias.isNotBlank() && cust.alias != cust.name) {
                                    Text(text = "Alias: ${cust.alias}", fontSize = 11.sp, color = PosTextSecondary)
                                }
                                if (cust.phone.isNotBlank()) {
                                    Text(text = "Tel: ${cust.phone}", fontSize = 11.sp, color = PosTextSecondary)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalPurchases)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = PosGreenAction
                            )
                            Text(
                                text = "${customerSales.size} tickets",
                                fontSize = 11.sp,
                                color = PosTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showAddDialog || customerToEdit != null) {
        val editing = customerToEdit
        var nameInput by remember { mutableStateOf(editing?.name ?: "") }
        var aliasInput by remember { mutableStateOf(editing?.alias ?: "") }
        var phoneInput by remember { mutableStateOf(editing?.phone ?: "") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                customerToEdit = null
            },
            containerColor = PosBackgroundSecondary,
            titleContentColor = PosTextPrimary,
            textContentColor = PosTextSecondary,
            title = { Text(if (editing == null) "+ NUEVO CLIENTE" else "EDITAR CLIENTE", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; isError = false },
                        label = { Text("Nombre Completo *") },
                        isError = isError,
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
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        label = { Text("Alias / Apodo") },
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
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Teléfono / WhatsApp") },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.trim().isBlank()) {
                            isError = true
                        } else {
                            val newCustomer = Customer(
                                id = editing?.id ?: UUID.randomUUID().toString(),
                                name = nameInput.trim(),
                                alias = if (aliasInput.trim().isBlank()) nameInput.trim() else aliasInput.trim(),
                                phone = phoneInput.trim()
                            )
                            viewModel.saveCustomer(newCustomer, isNew = (editing == null))
                            showAddDialog = false
                            customerToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Text("Guardar", color = PosBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; customerToEdit = null }) {
                    Text("Cancelar", color = PosTextSecondary)
                }
            }
        )
    }
}

// ----------------------------------------------------
// USERS & ROLES SCREEN
// ----------------------------------------------------
@Composable
fun UsersScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showCreateModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CONTROL DE ACCESO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Usuarios y Roles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextPrimary
                    )
                }

                Button(
                    onClick = { showCreateModal = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PosBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosBackground)
                }
            }
        }

        // Active Session Badge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanelSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosGreenAction)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PosGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentUser.name.take(1).uppercase(), color = PosBackground, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Sesión Actual Activa", fontSize = 10.sp, color = PosGreenActive, fontWeight = FontWeight.Bold)
                            Text(currentUser.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PosTextPrimary)
                            Text("Rol: ${currentUser.role}", fontSize = 11.sp, color = PosTextSecondary)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Lista de Usuarios del Sistema:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PosTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(users) { user ->
            val isCurrent = user.id == currentUser.id

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) PosGreenAction else PosBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PosTextPrimary)
                            if (isCurrent) {
                                Surface(color = PosGreenGlow, shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, PosGreenAction)) {
                                    Text("ACTUAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PosGreenActive, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("Usuario: @${user.username}", fontSize = 12.sp, color = PosTextSecondary)
                        Text("Rol: ${user.role}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PosGreenActive)
                    }

                    if (!isCurrent) {
                        Button(
                            onClick = { viewModel.switchUser(user) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosPanelSecondary, contentColor = PosTextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                        ) {
                            Text("Usar Cuenta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        var name by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("VENDEDOR") }
        val roles = listOf("ADMINISTRADOR", "SUPERVISOR", "VENDEDOR", "CAJERO")

        AlertDialog(
            onDismissRequest = { showCreateModal = false },
            containerColor = PosBackgroundSecondary,
            titleContentColor = PosTextPrimary,
            textContentColor = PosTextSecondary,
            title = { Text("+ NUEVO USUARIO", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre Completo *") },
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
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de Usuario (@)") },
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
                    Text("Rol:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        roles.forEach { role ->
                            val isSelected = selectedRole == role
                            Surface(
                                onClick = { selectedRole = role },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) PosGreenPrimary else PosPanel,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PosGreenActive else PosBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = role.take(4),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PosBackground else PosTextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && username.isNotBlank()) {
                            val newUser = User(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                username = username.trim().lowercase(),
                                role = selectedRole,
                                active = true
                            )
                            viewModel.saveUser(newUser, isNew = true)
                            showCreateModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                ) {
                    Text("Crear", color = PosBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateModal = false }) { Text("Cancelar", color = PosTextSecondary) }
            }
        )
    }
}

// ----------------------------------------------------
// AUDIT LOG SCREEN
// ----------------------------------------------------
@Composable
fun AuditScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.auditLogs.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SEGURIDAD Y CONTROL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Pista de Auditoría",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextPrimary
                )
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No hay eventos de auditoría registrados", color = PosTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(logs) { log ->
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault())
                val dateStr = sdf.format(Date(log.createdAt))

                val actionColor = when (log.action) {
                    "CREAR" -> PosGreenAction
                    "MODIFICAR" -> PosInfo
                    "DUPLICAR" -> PosGreenActive
                    "ANULAR", "ELIMINAR" -> PosError
                    else -> PosTextPrimary
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = PosPanelSecondary,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, actionColor)
                            ) {
                                Text(
                                    text = "${log.action} ${log.entity}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = actionColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = dateStr, fontSize = 10.sp, color = PosTextSecondary)
                        }

                        Text(
                            text = "Operador: ${log.userName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PosTextPrimary
                        )

                        if (!log.newValue.isNullOrBlank()) {
                            Text(
                                text = "Detalle: ${log.newValue}",
                                fontSize = 11.sp,
                                color = PosGreenActive
                            )
                        }
                        if (!log.oldValue.isNullOrBlank()) {
                            Text(
                                text = "Anterior: ${log.oldValue}",
                                fontSize = 10.sp,
                                color = PosTextDisabled
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SETTINGS SCREEN
// ----------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PREFERENCIAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Configuración del Sistema",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextPrimary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Configuración de Impresión Térmica", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PosTextPrimary)
                    Text("• Formato de papel: 58mm / 80mm POS Estándar", fontSize = 12.sp, color = PosTextSecondary)
                    Text("• Encabezado de Ticket: SISTEMA DE GESTIÓN DE SORTEOS", fontSize = 12.sp, color = PosTextSecondary)
                    Text("• Pie de Ticket: ¡Buena suerte con su jugada!", fontSize = 12.sp, color = PosTextSecondary)
                    Text("• Modo de corte: Automático", fontSize = 12.sp, color = PosTextSecondary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Acerca del Sistema POS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PosTextPrimary)
                    Text("Sistema Profesional de Gestión y Venta de Sorteos", fontSize = 12.sp, color = PosTextSecondary)
                    Text("Versión: 3.0.0 Dark Emerald Edition", fontSize = 11.sp, color = PosGreenActive, fontWeight = FontWeight.Bold)
                    Text("Base de Datos Local Room integrada para funcionamiento continuo y seguro.", fontSize = 11.sp, color = PosTextSecondary)
                }
            }
        }
    }
}

// ----------------------------------------------------
// CAMERA SCANNER SCREEN (Verificador Óptico & Cámara QR)
// ----------------------------------------------------
@Composable
fun CameraScannerScreen(
    viewModel: PosViewModel,
    onNavigateToTicket: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val resultsMap by viewModel.resultsMap.collectAsState()

    var manualCodeInput by remember { mutableStateOf("") }
    var selectedScannedSale by remember { mutableStateOf(sales.firstOrNull()) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "VERIFICADOR DE TICKETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Cámara & Escáner QR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextPrimary
                )
            }
        }

        // Camera Viewport Viewfinder
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B13)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PosGreenAction)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Grid background
                    Column(
                        modifier = Modifier
                            .size(170.dp)
                            .border(2.dp, PosGreenAction.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escáner",
                            tint = PosGreenActive.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enfoque el código QR",
                            fontSize = 10.sp,
                            color = PosGreenActive,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Laser Scanning Line
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = (40 + (140 * laserOffset)).dp)
                            .width(160.dp)
                            .height(2.dp)
                            .background(PosGreenAction)
                    )

                    // Flash and Camera controls top overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { flashEnabled = !flashEnabled },
                            shape = CircleShape,
                            color = if (flashEnabled) PosGreenAction else PosPanelSecondary.copy(alpha = 0.8f)
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (flashEnabled) PosBackground else PosTextPrimary,
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }

                        Surface(
                            onClick = { isBackCamera = !isBackCamera },
                            shape = CircleShape,
                            color = PosPanelSecondary.copy(alpha = 0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Cámara",
                                tint = PosTextPrimary,
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }
                    }

                    // Status pill at bottom
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PosPanel.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                    ) {
                        Text(
                            text = if (flashEnabled) "LINTERNA ACTIVA • LECTURA ÓPTICA" else "CÁMARA TRASERA ACTIVA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosGreenActive,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Quick Scan Simulator Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECCIONAR TICKET PARA ESCANEAR:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextSecondary,
                    letterSpacing = 0.5.sp
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sales) { saleItem ->
                        val isSelected = selectedScannedSale?.sale?.id == saleItem.sale.id
                        Surface(
                            onClick = {
                                selectedScannedSale = saleItem
                                manualCodeInput = saleItem.sale.ticketNumber
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PosGreenPrimary else PosPanel,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PosGreenActive else PosBorder
                            )
                        ) {
                            Text(
                                text = "${saleItem.sale.ticketNumber} (${saleItem.sale.customerName})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PosBackground else PosTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Scanned Ticket Result Card
        selectedScannedSale?.let { saleItem ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosGreenAction)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TICKET ESCANEADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosGreenActive)
                                Text(saleItem.sale.ticketNumber, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PosTextPrimary)
                            }

                            val isWinner = saleItem.items.any { item ->
                                val res = resultsMap[item.drawId]
                                res != null && (res.firstPrize == item.number || res.secondPrize == item.number || res.thirdPrize == item.number)
                            }

                            if (isWinner) {
                                Surface(
                                    color = PosErrorLight,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosError)
                                ) {
                                    Text(
                                        text = "🏆 TICKET GANADOR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PosError,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = PosPanelSecondary,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                                ) {
                                    Text(
                                        text = "REGISTRADO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = PosBorder)

                        Text("Cliente: ${saleItem.sale.customerName}", fontSize = 12.sp, color = PosTextPrimary, fontWeight = FontWeight.Medium)
                        Text("Vendido por: ${saleItem.sale.userName}", fontSize = 11.sp, color = PosTextSecondary)
                        Text("Total Apostado: $${String.format(Locale.US, "%.2f", saleItem.sale.total)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PosGreenAction)

                        // Plays inside
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Jugadas contenidas:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosTextSecondary)
                            saleItem.items.forEach { itm ->
                                val res = resultsMap[itm.drawId]
                                val itemWon = res != null && (res.firstPrize == itm.number || res.secondPrize == itm.number || res.thirdPrize == itm.number)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (itemWon) PosGoldLight else PosPanelSecondary)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${itm.drawName} (${itm.drawTime}) - #${itm.number} [${itm.quantity}x]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (itemWon) PosGold else PosTextPrimary)
                                    Text(
                                        if (itemWon) "GANÓ +$${String.format(Locale.US, "%.2f", itm.quantity * 14.0)}" else "$${String.format(Locale.US, "%.2f", itm.total)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (itemWon) PosGold else PosTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { onNavigateToTicket() },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PosBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VER TICKET EN HISTORIAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PosBackground)
                        }
                    }
                }
            }
        }
    }
}

