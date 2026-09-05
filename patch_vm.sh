sed -i '/private val _activeField/i\
    private val _selectedChancePrice = MutableStateFlow(0.20)\n\
    val selectedChancePrice: StateFlow<Double> = _selectedChancePrice.asStateFlow()\n\
    fun setSelectedChancePrice(price: Double) {\n\
        _selectedChancePrice.value = price\n\
        if (_selectedModality.value == "CHANCE") {\n\
            _unitPrice.value = price\n\
        }\n\
    }' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
