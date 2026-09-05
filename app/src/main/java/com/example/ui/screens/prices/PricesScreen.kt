package com.example.ui.screens.prices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.data.model.PriceConfig
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.util.Locale

@Composable
fun PricesScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val draws by viewModel.draws.collectAsState()
    val prices by viewModel.prices.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser.role == "ADMINISTRADOR"

    var selectedDrawFilterId by remember { mutableStateOf<String?>("GLOBAL") }
    var priceToEdit by remember { mutableStateOf<PriceConfig?>(null) }

    val filteredPrices = prices.filter { price ->
        if (selectedDrawFilterId == "GLOBAL") {
            price.drawId == "GLOBAL"
        } else {
            price.drawId == selectedDrawFilterId
        }
    }

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
                    text = "CONFIGURACIÓN FINANCIERA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Matriz de Precios",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosTextPrimary
                )
            }
        }

        // Integrity Notice Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = PosGreenAction)
                    Column {
                        Text(
                            text = "Integridad de Precios Históricos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosGreenActive
                        )
                        Text(
                            text = "Las modificaciones aplican únicamente a ventas futuras. Los tickets emitidos conservan su precio histórico inmutable.",
                            fontSize = 11.sp,
                            color = PosTextSecondary
                        )
                    }
                }
            }
        }

        // Scope Filter Tabs (Global vs Sorteo Específico)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val isGlobal = selectedDrawFilterId == "GLOBAL"
                    Surface(
                        onClick = { selectedDrawFilterId = "GLOBAL" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isGlobal) PosGreenAction else PosPanel,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGlobal) PosGreenActive else PosBorder
                        )
                    ) {
                        Text(
                            text = "🌐 Precios Globales",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGlobal) PosBackground else PosTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                items(draws) { draw ->
                    val isSelected = selectedDrawFilterId == draw.id
                    Surface(
                        onClick = { selectedDrawFilterId = draw.id },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PosGreenAction else PosPanel,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PosGreenActive else PosBorder
                        )
                    ) {
                        Text(
                            text = "${draw.icon} ${draw.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PosBackground else PosTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Price Cards
        if (filteredPrices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay tarifas específicas configuradas para este sorteo (se usarán los precios globales).",
                            fontSize = 13.sp,
                            color = PosTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredPrices) { price ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PosPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${price.modality} (${price.digits} CIFRAS)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosTextPrimary
                            )
                            Text(
                                text = "Ámbito: ${if (price.drawId == "GLOBAL") "Global por defecto" else "Sorteo específico"}",
                                fontSize = 11.sp,
                                color = PosTextSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", price.unitPrice)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PosGreenAction
                            )

                            if (isAdmin) {
                                IconButton(
                                    onClick = { priceToEdit = price },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar precio", tint = PosGreenActive)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Price
    priceToEdit?.let { price ->
        var newUnitPrice by remember { mutableStateOf("${price.unitPrice}") }

        Dialog(onDismissRequest = { priceToEdit = null }) {
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "MODIFICAR TARIFA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosTextPrimary
                    )

                    Text(
                        text = "Modalidad: ${price.modality} (${price.digits} cifras)",
                        fontSize = 13.sp,
                        color = PosGreenActive,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = newUnitPrice,
                        onValueChange = { newUnitPrice = it },
                        label = { Text("Precio Unitario ($)") },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { priceToEdit = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosTextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val parsed = newUnitPrice.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    viewModel.updatePrice(price, parsed)
                                    priceToEdit = null
                                }
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
    }
}
