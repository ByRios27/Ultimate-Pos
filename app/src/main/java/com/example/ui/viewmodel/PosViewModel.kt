package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SaleWithItems
import com.example.data.model.*
import android.content.Context
import android.content.SharedPreferences
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)

    private val _payoutConfig = MutableStateFlow(loadPayoutConfig())
    val payoutConfig: StateFlow<PayoutConfig> = _payoutConfig.asStateFlow()

    private fun loadPayoutConfig(): PayoutConfig {
        val chancesStr = prefs.getString("chancePrices", "") ?: ""
        val chances = if (chancesStr.isNotEmpty()) PayoutConfig.deserializeChances(chancesStr) else listOf(
            com.example.data.model.ChancePriceConfig(price = 0.20, first = 11.0, second = 3.0, third = 2.0),
            com.example.data.model.ChancePriceConfig(price = 0.25, first = 14.0, second = 3.0, third = 2.0)
        )
        return PayoutConfig(
            chancePrices = chances,
            paleFirstSecond = prefs.getFloat("paleFirstSecond", 1000.0f).toDouble(),
            paleSecondThird = prefs.getFloat("paleSecondThird", 200.0f).toDouble(),
            paleFirstThird = prefs.getFloat("paleFirstThird", 1000.0f).toDouble(),
            tripletaMultiplier = prefs.getFloat("tripletaMultiplier", 20000.0f).toDouble(),
            billeteMultiplier = prefs.getFloat("billeteMultiplier", 4000.0f).toDouble()
        )
    }

    fun updatePayoutConfig(config: PayoutConfig) {
        prefs.edit().apply {
            putString("chancePrices", config.serializeChances())
            putFloat("paleFirstSecond", config.paleFirstSecond.toFloat())
            putFloat("paleSecondThird", config.paleSecondThird.toFloat())
            putFloat("paleFirstThird", config.paleFirstThird.toFloat())
            putFloat("tripletaMultiplier", config.tripletaMultiplier.toFloat())
            putFloat("billeteMultiplier", config.billeteMultiplier.toFloat())
            apply()
        }
        _payoutConfig.value = config
    }

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val drawRepo = DrawRepository(db.drawDao(), db.auditDao())
    private val priceRepo = PriceRepository(db.priceDao(), db.auditDao())
    private val customerRepo = CustomerRepository(db.customerDao())
    private val userRepo = UserRepository(db.userDao(), db.auditDao())
    private val saleRepo = SalesRepository(db.saleDao(), db.auditDao(), db.customerDao())
    private val auditRepo = AuditRepository(db.auditDao())
    private val drawResultRepo = DrawResultRepository(db.drawResultDao())

    // All active data flows
    val draws: StateFlow<List<Draw>> = drawRepo.allDraws
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<DrawResult>> = drawResultRepo.allResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resultsMap: StateFlow<Map<String, DrawResult>> = results.map { list ->
        list.associateBy { it.drawId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    val activeDraws: StateFlow<List<Draw>> = drawRepo.activeDraws
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prices: StateFlow<List<PriceConfig>> = priceRepo.allPrices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepo.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = userRepo.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleWithItems>> = saleRepo.allSalesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSalesAmount: StateFlow<Double> = sales.map { list ->
        list.filter { it.sale.status == "ACTIVA" }.sumOf { it.sale.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCommissionAmount: StateFlow<Double> = sales.map { list ->
        list.filter { it.sale.status == "ACTIVA" }.sumOf { it.sale.commission }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalanceAmount: StateFlow<Double> = sales.map { list ->
        val active = list.filter { it.sale.status == "ACTIVA" }
        val total = active.sumOf { it.sale.total }
        val commission = active.sumOf { it.sale.commission }
        total - commission
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val auditLogs: StateFlow<List<AuditLog>> = auditRepo.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current logged-in user and auth state
    private val _currentUser = MutableStateFlow(
        User(
            id = "user_admin_01",
            name = "Admin Sistema",
            username = "admin",
            role = "ADMINISTRADOR",
            active = true
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // POS Active Focus: "NUMBER" or "QUANTITY"
    private val _selectedChancePrice = MutableStateFlow(0.20)

    val selectedChancePrice: StateFlow<Double> = _selectedChancePrice.asStateFlow()

    fun setSelectedChancePrice(price: Double) {

        _selectedChancePrice.value = price

        if (_selectedModality.value == "CHANCE") {

            _unitPrice.value = price

        }

    }
    private val _activeField = MutableStateFlow("NUMBER")
    val activeField: StateFlow<String> = _activeField.asStateFlow()

    private val _selectedDraw = MutableStateFlow<Draw?>(null)
    val selectedDraw: StateFlow<Draw?> = _selectedDraw.asStateFlow()

    private val _isMultiMode = MutableStateFlow(false)
    val isMultiMode: StateFlow<Boolean> = _isMultiMode.asStateFlow()

    private val _selectedMultiDrawIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMultiDrawIds: StateFlow<Set<String>> = _selectedMultiDrawIds.asStateFlow()

    private val _selectedModality = MutableStateFlow("CHANCE")
    val selectedModality: StateFlow<String> = _selectedModality.asStateFlow()

    private val _currentNumber = MutableStateFlow("")
    val currentNumber: StateFlow<String> = _currentNumber.asStateFlow()

    private val _quantityInput = MutableStateFlow("")
    val quantityInput: StateFlow<String> = _quantityInput.asStateFlow()

    private val _currentQuantity = MutableStateFlow(1.0)
    val currentQuantity: StateFlow<Double> = _currentQuantity.asStateFlow()

    private val _unitPrice = MutableStateFlow(0.20)
    val unitPrice: StateFlow<Double> = _unitPrice.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _latestSaleReceipt = MutableStateFlow<SaleWithItems?>(null)
    val latestSaleReceipt: StateFlow<SaleWithItems?> = _latestSaleReceipt.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // SALE EDITING STATE
    private val _editingSale = MutableStateFlow<SaleWithItems?>(null)
    val editingSale: StateFlow<SaleWithItems?> = _editingSale.asStateFlow()

    // History Filters
    val historySearchQuery = MutableStateFlow("")
    val historyFilterDrawId = MutableStateFlow<String?>(null)
    val historyFilterStatus = MutableStateFlow<String?>(null)
    val historyFilterModality = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            AppDatabase.ensureInitialData(db)
        }

        // Automatically select the first active draw when draws list becomes available
        viewModelScope.launch {
            activeDraws.collect { list ->
                if (_selectedDraw.value == null && list.isNotEmpty()) {
                    _selectedDraw.value = list.first()
                    updateUnitPrice()
                }
            }
        }
    }

    fun login(user: User) {
        _currentUser.value = user
        _isAuthenticated.value = true
        showSnackbar("Bienvenido(a), ${user.name}")
    }

    fun logout() {
        _isAuthenticated.value = false
        showSnackbar("Sesión cerrada")
    }

    fun switchUser(user: User) {
        _currentUser.value = user
        showSnackbar("Sesión cambiada a: ${user.name} (${user.role})")
    }

    fun selectDraw(draw: Draw) {
        _selectedDraw.value = draw
        // If current modality is not supported by new draw, switch to first supported
        if (!draw.hasModality(_selectedModality.value)) {
            val supported = draw.allowedModalities.split(",").map { it.trim().uppercase() }
            if (supported.isNotEmpty()) {
                _selectedModality.value = supported.first()
            }
        }
        validateAndTrimNumber(draw)
        updateUnitPrice()
    }

    fun toggleMultiMode() {
        val newMulti = !_isMultiMode.value
        _isMultiMode.value = newMulti
        if (newMulti) {
            val initial = mutableSetOf<String>()
            _selectedDraw.value?.let { initial.add(it.id) }
            _selectedMultiDrawIds.value = initial
        }
    }

    fun toggleDrawInMulti(drawId: String) {
        val current = _selectedMultiDrawIds.value.toMutableSet()
        if (current.contains(drawId)) {
            if (current.size > 1) {
                current.remove(drawId)
            }
        } else {
            current.add(drawId)
        }
        _selectedMultiDrawIds.value = current
    }

    fun selectModality(mod: String) {
        if (!_isMultiMode.value) {
            val draw = _selectedDraw.value
            if (draw != null && !draw.hasModality(mod)) {
                showSnackbar("El sorteo ${draw.name} no permite la modalidad $mod")
                return
            }
        } else {
            val selectedDraws = activeDraws.value.filter { _selectedMultiDrawIds.value.contains(it.id) }
            val unsupported = selectedDraws.firstOrNull { !it.hasModality(mod) }
            if (unsupported != null) {
                showSnackbar("El sorteo ${unsupported.name} no permite la modalidad $mod")
                return
            }
        }
        _selectedModality.value = mod
        _currentNumber.value = ""
        _quantityInput.value = ""
        _currentQuantity.value = 1.0
        _activeField.value = "NUMBER"
        updateUnitPrice()
    }

    private var lastKey = ""
    private var lastKeyTime = 0L

    fun setActiveField(field: String) {
        _activeField.value = field
        if (field == "QUANTITY") {
            _quantityInput.value = ""
        }
    }

    fun appendDigit(digit: String) {
        val now = System.currentTimeMillis()
        // Prevent duplicate events for the same key (e.g. browser synthetic double-tap)
        if (digit == lastKey && now - lastKeyTime < 350L) {
            return
        }
        if (now - lastKeyTime < 80L) {
            return
        }
        lastKey = digit
        lastKeyTime = now

        if (_activeField.value == "NUMBER") {
            appendDigitToNumber(digit)
        } else {
            appendDigitToQuantity(digit)
        }
    }

    private fun appendDigitToNumber(digit: String) {
        val current = _currentNumber.value
        val draw = _selectedDraw.value
        val maxAllowed = draw?.maxDigits() ?: 4

        if (_selectedModality.value == "PALE") {
            if (digit == ".") {
                if (!current.contains("-") && current.isNotEmpty()) {
                    _currentNumber.value = "$current-"
                }
                return
            }
            val digitsOnly = current.replace("-", "")
            if (digitsOnly.length < 4) {
                val newNumber = if (digitsOnly.length == 2 && !current.contains("-")) {
                    "$current-$digit"
                } else {
                    current + digit
                }
                _currentNumber.value = newNumber
                updateUnitPrice()

                // If Palé has completed 4 digits (e.g. 25-78), prepare quantity cleanly
                if (newNumber.replace("-", "").length == 4) {
                    _activeField.value = "QUANTITY"
                    _quantityInput.value = ""
                }
            }
        } else {
            // Chance: auto-switch to QUANTITY when 2 digits are reached
            
            if (current.length < maxAllowed) {
                val newNumber = current + digit
                _currentNumber.value = newNumber
                updateUnitPrice()

                if (newNumber.length == maxAllowed) {
                    _activeField.value = "QUANTITY"
                    _quantityInput.value = ""
                }
            }
        }
    }

    private fun appendDigitToQuantity(digit: String) {
        val currentStr = _quantityInput.value
        if (currentStr.length >= 7) return
        
        // Prevent multiple dots, and prevent dots entirely for CHANCE
        if (digit == ".") {
            if (_selectedModality.value == "CHANCE") return
            if (currentStr.contains(".")) return
        }
        
        val newStr = if (currentStr.isEmpty()) digit else currentStr + digit
        
        // Temporarily allow "0." or "." to be typed without strict double parsing failure
        if (newStr == "." || newStr == "0.") {
            _quantityInput.value = "0."
            return
        }

        val parsed = newStr.toDoubleOrNull()
        if (parsed != null && parsed in 0.01..9999.0) {
            _quantityInput.value = newStr
            _currentQuantity.value = parsed
        }
    }

    fun backspaceDigit() {
        val now = System.currentTimeMillis()
        if (lastKey == "⌫" && now - lastKeyTime < 350L) return
        if (now - lastKeyTime < 80L) return
        lastKey = "⌫"
        lastKeyTime = now

        if (_activeField.value == "NUMBER") {
            val current = _currentNumber.value
            if (current.isNotEmpty()) {
                if (current.endsWith("-")) {
                    _currentNumber.value = current.dropLast(2)
                } else {
                    _currentNumber.value = current.dropLast(1)
                }
                updateUnitPrice()
            }
        } else {
            val currentStr = _quantityInput.value
            if (currentStr.isNotEmpty()) {
                val newStr = currentStr.dropLast(1)
                _quantityInput.value = newStr
                _currentQuantity.value = newStr.toDoubleOrNull() ?: 1.0
            } else {
                _activeField.value = "NUMBER"
            }
        }
    }

    fun clearNumber() {
        _currentNumber.value = ""
        _quantityInput.value = ""
        _currentQuantity.value = 1.0
        _activeField.value = "NUMBER"
        updateUnitPrice()
    }

    fun setQuantity(qty: Double) {
        if (qty in 0.01..9999.0) {
            _quantityInput.value = qty.toString()
            _currentQuantity.value = qty
        }
    }

    fun addQuickQuantity(increment: Double) {
        val newQty = _currentQuantity.value + increment
        if (newQty in 0.01..9999.0) {
            _quantityInput.value = newQty.toString()
            _currentQuantity.value = newQty
        }
    }

    fun multiplyQuantity(multiplier: Double) {
        val newQty = _currentQuantity.value * multiplier
        if (newQty in 0.01..9999.0) {
            _quantityInput.value = newQty.toString()
            _currentQuantity.value = newQty
        }
    }

    fun resetQuantity() {
        _quantityInput.value = ""
        _currentQuantity.value = 1.0
    }

    fun incrementQuantity() {
        if (_currentQuantity.value < 9999) {
            _currentQuantity.value += 1
        }
    }

    fun decrementQuantity() {
        if (_currentQuantity.value > 1) {
            _currentQuantity.value -= 1
        }
    }

    private fun validateAndTrimNumber(draw: Draw) {
        val maxAllowed = draw.maxDigits()
        if (_selectedModality.value == "CHANCE" && _currentNumber.value.length > maxAllowed) {
            _currentNumber.value = _currentNumber.value.take(maxAllowed)
        }
    }

    private fun updateUnitPrice() {

        val draw = _selectedDraw.value ?: return

        val mod = _selectedModality.value

        val num = _currentNumber.value.replace("-", "")

        val digitsCount = if (num.isEmpty()) 2 else num.length

        if (mod == "CHANCE") {

            _unitPrice.value = _selectedChancePrice.value

            return

        }

        viewModelScope.launch {

            val price = priceRepo.getUnitPrice(draw.id, mod, digitsCount)

            _unitPrice.value = price

        }

    }

    fun addToCart() {
        val num = _currentNumber.value.trim()
        if (num.isEmpty()) {
            showSnackbar("Ingresa un número antes de agregar al ticket")
            return
        }

        if (_selectedModality.value == "PALE") {
            val clean = num.replace("-", "")
            if (clean.length < 2) {
                showSnackbar("Ingresa los números completos para el Palé (ej. 25-78)")
                return
            }
        } else if (_selectedModality.value == "CHANCE") {
            if (!_isMultiMode.value) {
                val draw = _selectedDraw.value
                val maxAllowed = draw?.maxDigits() ?: 4
                if (num.length != maxAllowed) {
                    showSnackbar("El sorteo requiere exactamente $maxAllowed cifras para CHANCE")
                    return
                }
            } else {
                val targetDraws = activeDraws.value.filter { _selectedMultiDrawIds.value.contains(it.id) }
                val unsupported = targetDraws.filter { it.maxDigits() != num.length }
                if (unsupported.isNotEmpty()) {
                    showSnackbar("El sorteo ${unsupported.first().name} requiere exactamente ${unsupported.first().maxDigits()} cifras")
                    return
                }
            }
        }

        val digitsCount = if (_selectedModality.value == "PALE") 2 else num.replace("-", "").length
        val quantity = _currentQuantity.value.coerceIn(0.01, 9999.0)

        if (_isMultiMode.value) {
            val targetDrawIds = _selectedMultiDrawIds.value
            val targetDraws = activeDraws.value.filter { targetDrawIds.contains(it.id) }

            if (targetDraws.isEmpty()) {
                showSnackbar("Selecciona al menos un sorteo en el modo multi")
                return
            }

            viewModelScope.launch {
                val currentCart = _cartItems.value.toMutableList()
                var mergedCount = 0
                var addedCount = 0

                for (draw in targetDraws) {
                    val existingIndex = currentCart.indexOfFirst {
                        it.draw.id == draw.id &&
                                it.modality.equals(_selectedModality.value, ignoreCase = true) &&
                                it.number.trim() == num
                    }

                    if (existingIndex >= 0) {
                        val existing = currentCart[existingIndex]
                        val newQty = (existing.quantity + quantity).coerceIn(0.01, 9999.0)
                        currentCart[existingIndex] = existing.copy(
                            quantity = newQty,
                            total = newQty * existing.unitPrice
                        )
                        mergedCount++
                    } else {
                        val price = if (_selectedModality.value == "CHANCE") _selectedChancePrice.value else priceRepo.getUnitPrice(draw.id, _selectedModality.value, digitsCount)
                        currentCart.add(
                            CartItem(
                                draw = draw,
                                modality = _selectedModality.value,
                                number = num,
                                digits = digitsCount,
                                quantity = quantity,
                                unitPrice = price,
                                total = quantity * price
                            )
                        )
                        addedCount++
                    }
                }

                _cartItems.value = currentCart

                if (mergedCount > 0 && addedCount == 0) {
                    showSnackbar("Número $num sumado en $mergedCount sorteos (+$quantity pzs)")
                } else if (mergedCount > 0) {
                    showSnackbar("Número $num agregado y sumado en ${targetDraws.size} sorteos")
                } else {
                    showSnackbar("Agregado a ${targetDraws.size} sorteos correctamente")
                }
            }
        } else {
            val draw = _selectedDraw.value
            if (draw == null) {
                showSnackbar("Selecciona un sorteo activo")
                return
            }

            viewModelScope.launch {
                val currentCart = _cartItems.value.toMutableList()
                val existingIndex = currentCart.indexOfFirst {
                    it.draw.id == draw.id &&
                            it.modality.equals(_selectedModality.value, ignoreCase = true) &&
                            it.number.trim() == num
                }

                if (existingIndex >= 0) {
                    val existing = currentCart[existingIndex]
                    val newQty = (existing.quantity + quantity).coerceIn(0.01, 9999.0)
                    currentCart[existingIndex] = existing.copy(
                        quantity = newQty,
                        total = newQty * existing.unitPrice
                    )
                    _cartItems.value = currentCart
                    showSnackbar("Número $num ya existía: se sumaron $quantity pzs (Total: $newQty pzs)")
                } else {
                    val price = if (_selectedModality.value == "CHANCE") _selectedChancePrice.value else priceRepo.getUnitPrice(draw.id, _selectedModality.value, digitsCount)
                    currentCart.add(
                        CartItem(
                            draw = draw,
                            modality = _selectedModality.value,
                            number = num,
                            digits = digitsCount,
                            quantity = quantity,
                            unitPrice = price,
                            total = quantity * price
                        )
                    )
                    _cartItems.value = currentCart
                    showSnackbar("Jugada agregada al ticket ($num - ${_selectedModality.value})")
                }
            }
        }

        _currentNumber.value = ""
        _quantityInput.value = ""
        _currentQuantity.value = 1.0
        _activeField.value = "NUMBER"
    }

    fun removeFromCart(itemId: String) {
        _cartItems.value = _cartItems.value.filter { it.id != itemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun updateCartItemQuantity(itemId: String, newQuantity: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = current[index]
            val validQty = newQuantity.coerceIn(0.01, 9999.0)
            current[index] = item.copy(
                quantity = validQty,
                total = validQty * item.unitPrice
            )
            _cartItems.value = current
        }
    }

    fun incrementCartItemQuantity(itemId: String) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + 1
            current[index] = item.copy(
                quantity = newQty,
                total = newQty * item.unitPrice
            )
            _cartItems.value = current
        }
    }

    fun decrementCartItemQuantity(itemId: String) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity > 1) {
                val newQty = item.quantity - 1
                current[index] = item.copy(
                    quantity = newQty,
                    total = newQty * item.unitPrice
                )
                _cartItems.value = current
            } else {
                removeFromCart(itemId)
            }
        }
    }

    fun updateCartItem(itemId: String, newNumber: String, newQuantity: Double, newModality: String? = null) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = current[index]
            val modalityToUse = newModality ?: item.modality
            val cleanNum = newNumber.trim()
            val digitsCount = if (modalityToUse == "PALE") 2 else cleanNum.replace("-", "").length

            viewModelScope.launch {
                val price = if (modalityToUse == "CHANCE") _selectedChancePrice.value else priceRepo.getUnitPrice(item.draw.id, modalityToUse, digitsCount)
                val targetQty = newQuantity.coerceIn(0.01, 9999.0)

                // Check if another item in the same draw has this same number and modality
                val duplicateIndex = current.indexOfFirst {
                    it.id != itemId &&
                            it.draw.id == item.draw.id &&
                            it.modality.equals(modalityToUse, ignoreCase = true) &&
                            it.number.trim() == cleanNum
                }

                if (duplicateIndex >= 0) {
                    val duplicate = current[duplicateIndex]
                    val mergedQty = (duplicate.quantity + targetQty).coerceIn(0.01, 9999.0)
                    current[duplicateIndex] = duplicate.copy(
                        quantity = mergedQty,
                        unitPrice = price,
                        total = mergedQty * price
                    )
                    current.removeAt(index)
                    showSnackbar("Se combinó $cleanNum con la jugada existente (Total: $mergedQty pzs)")
                } else {
                    val updated = item.copy(
                        number = cleanNum,
                        modality = modalityToUse,
                        digits = digitsCount,
                        quantity = targetQty,
                        unitPrice = price,
                        total = targetQty * price
                    )
                    current[index] = updated
                    showSnackbar("Jugada actualizada: $cleanNum ($modalityToUse) x$targetQty")
                }
                _cartItems.value = current
            }
        }
    }

    fun finalizeSale(customerName: String, onComplete: (SaleWithItems) -> Unit) {
        if (customerName.isBlank()) {
            showSnackbar("El nombre del cliente es obligatorio")
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) {
            showSnackbar("El ticket no tiene jugadas")
            return
        }

        viewModelScope.launch {
            try {
                val saleWithItems = saleRepo.createSale(
                    customerName = customerName,
                    currentUser = _currentUser.value,
                    cartItems = items,
                    commissionRate = 0.05
                )
                _cartItems.value = emptyList()
                _latestSaleReceipt.value = saleWithItems
                onComplete(saleWithItems)
                showSnackbar("Venta confirmada exitosamente: ${saleWithItems.sale.ticketNumber}")
            } catch (e: Exception) {
                showSnackbar("Error al procesar la venta: ${e.message}")
            }
        }
    }

    fun dismissReceipt() {
        _latestSaleReceipt.value = null
    }

    // DRAW MANAGEMENT
    fun saveDraw(draw: Draw, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) {
                drawRepo.insertDraw(draw, _currentUser.value)
                // Seed default prices for this new draw
                for (digits in 1..4) {
                    priceRepo.saveOrUpdatePrice(
                        PriceConfig(
                            drawId = draw.id,
                            modality = "CHANCE",
                            digits = digits,
                            unitPrice = 0.20
                        ),
                        _currentUser.value
                    )
                }
                for (digits in 2..4) {
                    priceRepo.saveOrUpdatePrice(
                        PriceConfig(
                            drawId = draw.id,
                            modality = "PALE",
                            digits = digits,
                            unitPrice = 0.20
                        ),
                        _currentUser.value
                    )
                }
                showSnackbar("Sorteo ${draw.name} creado con éxito")
            } else {
                val existing = drawRepo.getDrawById(draw.id)
                if (existing != null) {
                    drawRepo.updateDraw(existing, draw, _currentUser.value)
                    showSnackbar("Sorteo ${draw.name} actualizado")
                }
            }
        }
    }

    fun duplicateDraw(source: Draw, newName: String, newTime: String, newClosingTime: String) {
        viewModelScope.launch {
            val duplicated = drawRepo.duplicateDraw(source, newName, newTime, newClosingTime, _currentUser.value)
            // Copy prices
            for (digits in 1..4) {
                priceRepo.saveOrUpdatePrice(
                    PriceConfig(
                        drawId = duplicated.id,
                        modality = "CHANCE",
                        digits = digits,
                        unitPrice = 0.20
                    ),
                    _currentUser.value
                )
            }
            showSnackbar("Sorteo duplicado como '$newName'")
        }
    }

    fun toggleDrawActive(draw: Draw) {
        viewModelScope.launch {
            drawRepo.toggleActive(draw, _currentUser.value)
            showSnackbar("Estado de ${draw.name} actualizado")
        }
    }

    fun deleteDraw(draw: Draw) {
        viewModelScope.launch {
            drawRepo.deleteDraw(draw, _currentUser.value)
            priceRepo.deletePricesForDraw(draw.id)
            showSnackbar("Sorteo ${draw.name} eliminado")
        }
    }

    // PRICE MANAGEMENT
    fun updatePrice(price: PriceConfig, newUnitPrice: Double) {
        viewModelScope.launch {
            val updated = price.copy(unitPrice = newUnitPrice, updatedAt = System.currentTimeMillis())
            priceRepo.saveOrUpdatePrice(updated, _currentUser.value)
            showSnackbar("Precio actualizado a $${String.format("%.2f", newUnitPrice)}")
            updateUnitPrice()
        }
    }

    // VOID / ANULAR SALE
    fun voidSale(saleId: String, reason: String) {
        viewModelScope.launch {
            saleRepo.voidSale(saleId, reason, _currentUser.value)
            showSnackbar("Venta anulada correctamente")
        }
    }

    // UPDATE SALE CUSTOMER
    fun updateSaleCustomer(saleId: String, newCustomerName: String) {
        viewModelScope.launch {
            saleRepo.updateSaleCustomerName(saleId, newCustomerName, _currentUser.value)
            showSnackbar("Cliente actualizado a: $newCustomerName")
        }
    }

    // UPDATE SALE ITEM
    fun updateSaleItem(item: SaleItem, newNumber: String, newQuantity: Double) {
        viewModelScope.launch {
            val validQty = newQuantity.coerceIn(0.01, 9999.0)
            val updated = item.copy(
                number = newNumber.trim(),
                quantity = validQty,
                total = validQty * item.unitPrice
            )
            saleRepo.updateSaleItem(updated, _currentUser.value)
            showSnackbar("Jugada #${item.number} actualizada")
        }
    }

    // SALE EDITING WORKFLOW
    fun startEditingSale(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            val allDrawsList = draws.value
            _editingSale.value = saleWithItems

            // Load the ticket plays into the POS cart
            val loadedCart = saleWithItems.items.map { item ->
                val draw = allDrawsList.find { it.id == item.drawId } ?: Draw(
                    id = item.drawId,
                    name = item.drawName,
                    icon = item.drawIcon,
                    drawTime = item.drawTime,
                    closingTime = "11:59 PM"
                )
                CartItem(
                    id = item.id,
                    draw = draw,
                    modality = item.modality,
                    number = item.number,
                    digits = item.digits,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    total = item.total
                )
            }
            _cartItems.value = loadedCart

            // Select the draw of the first item if available
            saleWithItems.items.firstOrNull()?.let { firstItem ->
                allDrawsList.find { it.id == firstItem.drawId }?.let { matchedDraw ->
                    _selectedDraw.value = matchedDraw
                    _selectedModality.value = firstItem.modality
                }
            }

            showSnackbar("Editando ticket #${saleWithItems.sale.ticketNumber} - Puedes modificar jugadas y confirmar")
        }
    }

    fun cancelEditingSale() {
        _editingSale.value = null
        _cartItems.value = emptyList()
        showSnackbar("Edición de ticket cancelada")
    }

    fun confirmEditedSale(customerName: String, onComplete: (SaleWithItems) -> Unit) {
        val editing = _editingSale.value
        if (editing == null) {
            showSnackbar("No hay una venta en modo edición")
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) {
            showSnackbar("El ticket editado debe tener al menos una jugada")
            return
        }

        val targetCustomer = customerName.ifBlank { editing.sale.customerName }

        viewModelScope.launch {
            try {
                val updated = saleRepo.updateSaleWithItems(
                    saleId = editing.sale.id,
                    customerName = targetCustomer,
                    currentUser = _currentUser.value,
                    cartItems = items,
                    commissionRate = 0.05
                )
                _editingSale.value = null
                _cartItems.value = emptyList()
                _latestSaleReceipt.value = updated
                onComplete(updated)
                showSnackbar("Ticket #${updated.sale.ticketNumber} modificado exitosamente")
            } catch (e: Exception) {
                showSnackbar("Error al guardar edición: ${e.message}")
            }
        }
    }

    // REPEAT SALE (Duplicate plays directly into POS cart, optionally re-targeting a chosen Draw)
    fun repeatSaleToCart(saleWithItems: SaleWithItems, targetDraw: Draw? = null) {
        viewModelScope.launch {
            val allActiveDraws = draws.value
            val newCart = _cartItems.value.toMutableList()
            var addedCount = 0

            saleWithItems.items.forEach { saleItem ->
                val draw = targetDraw ?: allActiveDraws.find { it.id == saleItem.drawId } ?: Draw(
                    id = saleItem.drawId,
                    name = saleItem.drawName,
                    icon = saleItem.drawIcon,
                    drawTime = saleItem.drawTime,
                    closingTime = "11:59 PM"
                )

                // Recalculate price if target draw is different
                val unitPrice = if (targetDraw != null && targetDraw.id != saleItem.drawId) {
                    if (saleItem.modality == "CHANCE") _selectedChancePrice.value else priceRepo.getUnitPrice(targetDraw.id, saleItem.modality, saleItem.digits)
                } else {
                    saleItem.unitPrice
                }

                newCart.add(
                    CartItem(
                        draw = draw,
                        modality = saleItem.modality,
                        number = saleItem.number,
                        digits = saleItem.digits,
                        quantity = saleItem.quantity,
                        unitPrice = unitPrice,
                        total = saleItem.quantity * unitPrice
                    )
                )
                addedCount++
            }

            _cartItems.value = newCart
            val targetMsg = if (targetDraw != null) " al sorteo ${targetDraw.name}" else ""
            showSnackbar("Se cargaron $addedCount jugadas al carrito de venta$targetMsg")
        }
    }

    // CUSTOMER MANAGEMENT
    fun saveCustomer(customer: Customer, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) {
                customerRepo.insertCustomer(customer)
                showSnackbar("Cliente registrado")
            } else {
                customerRepo.updateCustomer(customer)
                showSnackbar("Cliente actualizado")
            }
        }
    }

    // USER MANAGEMENT
    fun saveUser(user: User, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) {
                userRepo.insertUser(user, _currentUser.value)
                showSnackbar("Usuario ${user.username} creado")
            } else {
                userRepo.updateUser(user, _currentUser.value)
                showSnackbar("Usuario ${user.username} actualizado")
            }
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }
}
