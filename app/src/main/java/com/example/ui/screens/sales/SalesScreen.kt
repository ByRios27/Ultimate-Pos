package com.example.ui.screens.sales

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun SalesScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDraw by viewModel.selectedDraw.collectAsState()
    val activeDraws by viewModel.activeDraws.collectAsState()
    val isMultiMode by viewModel.isMultiMode.collectAsState()
    val selectedMultiIds by viewModel.selectedMultiDrawIds.collectAsState()
    val selectedModality by viewModel.selectedModality.collectAsState()
    val currentNumber by viewModel.currentNumber.collectAsState()
    val quantityInput by viewModel.quantityInput.collectAsState()
    val currentQuantity by viewModel.currentQuantity.collectAsState()
    val unitPrice by viewModel.unitPrice.collectAsState()
    val activeField by viewModel.activeField.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val editingSale by viewModel.editingSale.collectAsState()

    var showDrawSelector by remember { mutableStateOf(false) }
    var showMultiSelector by remember { mutableStateOf(false) }
    var showCustomerModal by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .verticalScroll(scrollState)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Edición Banner if editing an existing sale
        editingSale?.let { saleWithItem ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "MODO EDICIÓN DE TICKET",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Ticket ${saleWithItem.sale.ticketNumber} • Cliente: ${saleWithItem.sale.customerName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFB45309),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.cancelEditingSale() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar edición",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancelar Edición",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Active Draw Section
        ActiveDrawSelector(
            selectedDraw = selectedDraw,
            isMultiMode = isMultiMode,
            selectedMultiDrawsCount = selectedMultiIds.size,
            onOpenSelector = {
                if (isMultiMode) {
                    showMultiSelector = true
                } else {
                    showDrawSelector = true
                }
            },
            onToggleMulti = {
                viewModel.toggleMultiMode()
                if (!isMultiMode) {
                    showMultiSelector = true
                }
            }
        )

        // Modality Selector: CHANCE | PALÉ
        val allowedModalities = if (isMultiMode) {
            val selectedDraws = activeDraws.filter { selectedMultiIds.contains(it.id) }
            if (selectedDraws.isNotEmpty()) {
                selectedDraws.map { it.allowedModalities.split(",").map { m -> m.trim().uppercase() }.filter { m -> m.isNotEmpty() }.toSet() }
                    .reduce { acc, set -> acc.intersect(set) }
                    .joinToString(",")
            } else {
                ""
            }
        } else {
            selectedDraw?.allowedModalities ?: ""
        }
        
        if (allowedModalities.isNotEmpty()) {
            val modes = allowedModalities.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            if (selectedModality !in modes && modes.isNotEmpty()) {
                LaunchedEffect(allowedModalities) {
                    viewModel.selectModality(modes.first())
                }
            }

            GameModeSelector(
                selectedModality = selectedModality,
                allowedModalities = allowedModalities,
                onSelectModality = { viewModel.selectModality(it) }
            )
        }



        if (selectedModality == "CHANCE") {
            val payoutConfig by viewModel.payoutConfig.collectAsState()
            val chancePrices = payoutConfig.chancePrices
            val selectedChancePrice by viewModel.selectedChancePrice.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Costo:",
                    fontSize = 11.sp,
                    color = PosTextSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .background(PosPanelSecondary, RoundedCornerShape(6.dp))
                        .border(1.dp, PosBorder, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    chancePrices.forEach { chanceConf ->
                        val isSelected = Math.abs(chanceConf.price - selectedChancePrice) < 0.01
                        Surface(
                            onClick = { viewModel.setSelectedChancePrice(chanceConf.price) },
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) PosGreenAction else Color.Transparent,
                        ) {
                            Text(
                                text = "$${chanceConf.price}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PosBackground else PosTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Number Input & Quantity Selector with Focus Highlight
        NumberDisplayAndQuantity(
            number = currentNumber,
            modality = selectedModality,
            quantity = currentQuantity,
            quantityInput = quantityInput,
            unitPrice = unitPrice,
            activeField = activeField,
            onSelectField = { viewModel.setActiveField(it) },
            onIncrementQty = { viewModel.incrementQuantity() },
            onDecrementQty = { viewModel.decrementQuantity() },
            onClearNumber = { viewModel.clearNumber() }
        )

        // Touch Keypad with Quick Quantities and Autofocus Switch
        NumberPad(
            onDigitClick = { viewModel.appendDigit(it) },
            onBackspace = { viewModel.backspaceDigit() },
            onAddPlay = { viewModel.addToCart() },
            onQuickAddQty = { viewModel.addQuickQuantity(it) },
            onMultiplyQty = { viewModel.multiplyQuantity(it) },
            onResetQty = { viewModel.resetQuantity() }
        )

        // Action Button: AGREGAR AL TICKET
        Button(
            onClick = { viewModel.addToCart() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PosGreenAction)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PosBackground,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AGREGAR JUGADA AL TICKET",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PosBackground,
                letterSpacing = 0.5.sp
            )
        }

        // Cart appears dynamically directly below the sales panel as plays are added
        if (cartItems.isNotEmpty()) {
            TicketCartInlineSection(
                cartItems = cartItems,
                onUpdateQuantity = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                onRemoveItem = { viewModel.removeFromCart(it) },
                onClearCart = { viewModel.clearCart() },
                onProceedToCustomer = { showCustomerModal = true },
                isEditing = editingSale != null,
                editingTicketNumber = editingSale?.sale?.ticketNumber,
                onCancelEdit = { viewModel.cancelEditingSale() },
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Modal Dialogs
    if (showDrawSelector) {
        DrawSelectionDialog(
            draws = activeDraws,
            selectedDrawId = selectedDraw?.id,
            onSelectDraw = { viewModel.selectDraw(it) },
            onDismiss = { showDrawSelector = false }
        )
    }

    if (showMultiSelector) {
        MultiDrawSelectionDialog(
            draws = activeDraws,
            selectedIds = selectedMultiIds,
            onToggleDraw = { viewModel.toggleDrawInMulti(it) },
            onDismiss = { showMultiSelector = false }
        )
    }

    if (showCustomerModal) {
        CustomerModal(
            savedCustomers = customers,
            initialCustomerName = editingSale?.sale?.customerName ?: "",
            isEditing = editingSale != null,
            ticketNumber = editingSale?.sale?.ticketNumber,
            onConfirm = { name ->
                showCustomerModal = false
                if (editingSale != null) {
                    viewModel.confirmEditedSale(name) {
                        // Edited sale confirmed and receipt updated
                    }
                } else {
                    viewModel.finalizeSale(name) {
                        // Sale finalized, receipt dialog will open automatically via lastSale state
                    }
                }
            },
            onDismiss = { showCustomerModal = false }
        )
    }

    if (showCartSheet) {
        TicketCartBottomSheet(
            cartItems = cartItems,
            onUpdateQuantity = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
            onRemoveItem = { viewModel.removeFromCart(it) },
            onClearCart = { viewModel.clearCart() },
            onProceedToCustomer = {
                showCartSheet = false
                showCustomerModal = true
            },
            onDismiss = { showCartSheet = false }
        )
    }
}
