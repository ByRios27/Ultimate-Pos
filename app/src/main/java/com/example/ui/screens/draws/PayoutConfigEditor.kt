package com.example.ui.screens.draws

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChancePriceConfig
import com.example.data.model.PayoutConfig
import com.example.ui.theme.*

@Composable
fun PayoutConfigEditor(config: PayoutConfig, onSave: (PayoutConfig) -> Unit) {
    var chancePrices by remember { mutableStateOf(config.chancePrices) }
    var paleFirstSecond by remember { mutableStateOf(config.paleFirstSecond.toString()) }
    var paleSecondThird by remember { mutableStateOf(config.paleSecondThird.toString()) }
    var paleFirstThird by remember { mutableStateOf(config.paleFirstThird.toString()) }
    var tripletaMultiplier by remember { mutableStateOf(config.tripletaMultiplier.toString()) }
    var billeteMultiplier by remember { mutableStateOf(config.billeteMultiplier.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Configuración de Premios", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PosTextPrimary)
            Text("Define los precios y pagos para las diferentes modalidades.", fontSize = 12.sp, color = PosTextSecondary)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Premios de CHANCES", fontWeight = FontWeight.Bold, color = PosGreenActive)
                TextButton(onClick = {
                    chancePrices = chancePrices + ChancePriceConfig(0.0, 0.0, 0.0, 0.0)
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PosGreenAction)
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir Precio", color = PosGreenAction)
                }
            }
        }

        itemsIndexed(chancePrices) { index, cp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (cp.price == 0.0) "" else cp.price.toString(),
                            onValueChange = { nv -> 
                                val newPrices = chancePrices.toMutableList()
                                newPrices[index] = cp.copy(price = nv.toDoubleOrNull() ?: 0.0)
                                chancePrices = newPrices
                            },
                            label = { Text("Precio del Chance ($)") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val newPrices = chancePrices.toMutableList()
                            newPrices.removeAt(index)
                            chancePrices = newPrices
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = PosError)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cp.first.toString(),
                            onValueChange = { nv -> 
                                val newPrices = chancePrices.toMutableList()
                                newPrices[index] = cp.copy(first = nv.toDoubleOrNull() ?: 0.0)
                                chancePrices = newPrices
                            },
                            label = { Text("1er Premio") }, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cp.second.toString(),
                            onValueChange = { nv -> 
                                val newPrices = chancePrices.toMutableList()
                                newPrices[index] = cp.copy(second = nv.toDoubleOrNull() ?: 0.0)
                                chancePrices = newPrices
                            },
                            label = { Text("2do Premio") }, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cp.third.toString(),
                            onValueChange = { nv -> 
                                val newPrices = chancePrices.toMutableList()
                                newPrices[index] = cp.copy(third = nv.toDoubleOrNull() ?: 0.0)
                                chancePrices = newPrices
                            },
                            label = { Text("3er Premio") }, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Premios de PALÉS (Multiplicador de inversión)", fontWeight = FontWeight.Bold, color = PosGreenActive)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = paleFirstSecond, onValueChange = { paleFirstSecond = it }, label = { Text("1ro y 2do") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = paleFirstThird, onValueChange = { paleFirstThird = it }, label = { Text("1ro y 3ro") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = paleSecondThird, onValueChange = { paleSecondThird = it }, label = { Text("2do y 3ro") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PosPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Otras Modalidades (Multiplicador de inversión)", fontWeight = FontWeight.Bold, color = PosGreenActive)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = tripletaMultiplier, onValueChange = { tripletaMultiplier = it }, label = { Text("Tripleta") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = billeteMultiplier, onValueChange = { billeteMultiplier = it }, label = { Text("Billete (Exacto)") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val newConfig = PayoutConfig(
                        chancePrices = chancePrices,
                        paleFirstSecond = paleFirstSecond.toDoubleOrNull() ?: config.paleFirstSecond,
                        paleSecondThird = paleSecondThird.toDoubleOrNull() ?: config.paleSecondThird,
                        paleFirstThird = paleFirstThird.toDoubleOrNull() ?: config.paleFirstThird,
                        tripletaMultiplier = tripletaMultiplier.toDoubleOrNull() ?: config.tripletaMultiplier,
                        billeteMultiplier = billeteMultiplier.toDoubleOrNull() ?: config.billeteMultiplier
                    )
                    onSave(newConfig)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
            ) {
                Text("Guardar Cambios", color = PosBackground, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
