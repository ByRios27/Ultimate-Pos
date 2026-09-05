package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.ResumenScreen
import com.example.ui.screens.draws.DrawsScreen
import com.example.ui.screens.history.TicketsHistoryScreen
import com.example.ui.screens.other.ArchivosScreen
import com.example.ui.screens.other.AuditScreen
import com.example.ui.screens.other.CameraScannerScreen
import com.example.ui.screens.other.CustomersScreen
import com.example.ui.screens.other.SettingsScreen
import com.example.ui.screens.other.UsersScreen
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PosBackground
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SorteosPosApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SorteosPosApp(viewModel: PosViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val usersList by viewModel.users.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val resultsMap by viewModel.resultsMap.collectAsState()
    val latestReceipt by viewModel.latestSaleReceipt.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val totalSales by viewModel.totalSalesAmount.collectAsState()
    val totalCommission by viewModel.totalCommissionAmount.collectAsState()
    val netBalance by viewModel.netBalanceAmount.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(AppDestination.VENTAS) }

    var showCartSheet by remember { mutableStateOf(false) }
    var showCustomerModal by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    if (!isAuthenticated) {
        LoginScreen(
            users = usersList,
            onLoginSuccess = { user ->
                viewModel.login(user)
            }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawerContent(
                    currentDestination = currentDestination,
                    currentUser = currentUser,
                    usersList = usersList,
                    onNavigate = { destination ->
                        currentDestination = destination
                    },
                    onSwitchUser = { user ->
                        viewModel.switchUser(user)
                    },
                    onLogout = {
                        viewModel.logout()
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    AppHeader(
                        totalSales = totalSales,
                        totalCommission = totalCommission,
                        netBalance = netBalance,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onLogoutClick = {
                            viewModel.logout()
                        }
                    )
                },
                bottomBar = {
                    AppBottomBar(
                        currentDestination = currentDestination,
                        onNavigate = { destination ->
                            currentDestination = destination
                        }
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                containerColor = PosBackground
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(PosBackground)
                ) {
                    when (currentDestination) {
                        AppDestination.VENTAS -> SalesScreen(viewModel = viewModel)
                        AppDestination.RESUMEN -> ResumenScreen(
                            viewModel = viewModel,
                            onNavigateToSales = { currentDestination = AppDestination.VENTAS }
                        )
                        AppDestination.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSales = { currentDestination = AppDestination.VENTAS }
                        )
                        AppDestination.TICKETS, AppDestination.HISTORIAL -> TicketsHistoryScreen(
                            viewModel = viewModel,
                            onNavigateToSales = { currentDestination = AppDestination.VENTAS }
                        )
                        AppDestination.ARCHIVOS -> ArchivosScreen(
                            viewModel = viewModel,
                            onNavigateToTickets = { currentDestination = AppDestination.HISTORIAL }
                        )
                        AppDestination.CAMARA -> CameraScannerScreen(
                            viewModel = viewModel,
                            onNavigateToTicket = { currentDestination = AppDestination.HISTORIAL }
                        )
                        AppDestination.SORTEOS -> DrawsScreen(viewModel = viewModel)
                        AppDestination.CLIENTES -> CustomersScreen(viewModel = viewModel)
                        AppDestination.USUARIOS -> UsersScreen(viewModel = viewModel)
                        AppDestination.AUDITORIA -> AuditScreen(viewModel = viewModel)
                        AppDestination.CONFIGURACION -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Cart Bottom Sheet
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

        // Customer Information Modal for sale checkout
        if (showCustomerModal) {
            CustomerModal(
                savedCustomers = customers,
                onConfirm = { customerName ->
                    showCustomerModal = false
                    viewModel.finalizeSale(customerName) {
                        // Receipt dialog automatically shown through latestReceipt state
                    }
                },
                onDismiss = { showCustomerModal = false }
            )
        }

        // Thermal Receipt Dialog on successful sale
        latestReceipt?.let { receipt ->
            ReceiptDialog(
                saleWithItems = receipt,
                resultsMap = resultsMap,
                onDismiss = { viewModel.dismissReceipt() },
                onNewSale = {
                    viewModel.dismissReceipt()
                    currentDestination = AppDestination.VENTAS
                }
            )
        }
    }
}
