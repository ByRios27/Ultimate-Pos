package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.*

enum class AppDestination(
    val title: String,
    val icon: ImageVector,
    val section: String
) {
    VENTAS("Ventas (POS)", Icons.Default.GridView, "PRINCIPAL"),
    HISTORIAL("Tickets & Historial", Icons.Default.ReceiptLong, "PRINCIPAL"),
    RESUMEN("Resumen", Icons.Default.Analytics, "PRINCIPAL"),
    DASHBOARD("Reportes", Icons.Default.BarChart, "PRINCIPAL"),
    ARCHIVOS("Archivos & Cierres", Icons.Default.Folder, "PRINCIPAL"),
    CAMARA("Cámara & Escáner", Icons.Default.CameraAlt, "PRINCIPAL"),
    SORTEOS("Gestión de Sorteos", Icons.Default.Casino, "GESTIÓN"),
    CLIENTES("Directorio de Clientes", Icons.Default.People, "DIRECTORIO"),
    USUARIOS("Usuarios & Roles", Icons.Default.ManageAccounts, "SISTEMA"),
    AUDITORIA("Pista de Auditoría", Icons.Default.Security, "SISTEMA"),
    CONFIGURACION("Configuración POS", Icons.Default.Settings, "SISTEMA"),
    TICKETS("Tickets & Ventas", Icons.Default.ConfirmationNumber, "PRINCIPAL")
}

@Composable
fun NavigationDrawerContent(
    currentDestination: AppDestination,
    currentUser: User,
    usersList: List<User>,
    onNavigate: (AppDestination) -> Unit,
    onSwitchUser: (User) -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = PosBackgroundSecondary,
        drawerContentColor = PosTextPrimary,
        modifier = Modifier.width(310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Brand Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PosPanel)
                            .border(1.dp, PosGreenAction, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = PosGreenAction,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SORTEOS POS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosTextPrimary
                        )
                        Text(
                            text = "Sistema de Gestión & Ventas",
                            fontSize = 11.sp,
                            color = PosGreenActive,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Divider(color = PosBorder, modifier = Modifier.padding(bottom = 12.dp))

                // Navigation Items Grouped
                val sections = listOf("PRINCIPAL", "GESTIÓN", "DIRECTORIO", "SISTEMA")

                sections.forEach { sec ->
                    Text(
                        text = sec,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )

                    AppDestination.values()
                        .filter { it.section == sec && it != AppDestination.TICKETS }
                        .forEach { destination ->
                            val isSelected = currentDestination == destination ||
                                    (destination == AppDestination.HISTORIAL && currentDestination == AppDestination.TICKETS)

                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title,
                                        tint = if (isSelected) PosBackground else PosTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) PosBackground else PosTextPrimary
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    onNavigate(destination)
                                    onCloseDrawer()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = PosGreenAction,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                }
            }

            // User Switcher and Logout Card at Bottom
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Divider(color = PosBorder, modifier = Modifier.padding(bottom = 12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PosGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.name.take(1).uppercase(),
                                    color = PosBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PosTextPrimary
                                )
                                Text(
                                    text = currentUser.role,
                                    fontSize = 10.sp,
                                    color = PosGreenActive,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Logout icon button
                            IconButton(
                                onClick = {
                                    onCloseDrawer()
                                    onLogout()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Cerrar sesión",
                                    tint = PosError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (usersList.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cambiar Usuario Rápido:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                usersList.forEach { user ->
                                    val isCurrent = user.id == currentUser.id
                                    Surface(
                                        onClick = { onSwitchUser(user) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrent) PosGreenPrimary else PosPanelSecondary,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) PosGreenActive else PosBorder
                                        )
                                    ) {
                                        Text(
                                            text = user.name.split(" ").first(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) PosBackground else PosTextPrimary,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
