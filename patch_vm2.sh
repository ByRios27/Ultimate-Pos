sed -i '/private fun updateUnitPrice() {/,/}/c\
    private fun updateUnitPrice() {\n\
        val draw = _selectedDraw.value ?: return\n\
        val mod = _selectedModality.value\n\
        val num = _currentNumber.value.replace("-", "")\n\
        val digitsCount = if (num.isEmpty()) 2 else num.length\n\
        if (mod == "CHANCE") {\n\
            _unitPrice.value = _selectedChancePrice.value\n\
            return\n\
        }\n\
        viewModelScope.launch {\n\
            val price = priceRepo.getUnitPrice(draw.id, mod, digitsCount)\n\
            _unitPrice.value = price\n\
        }\n\
    }' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
