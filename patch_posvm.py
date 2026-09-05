import re

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'r') as f:
    content = f.read()

# Replace selectModality to also check multi-mode
new_func = """
    fun selectModality(mod: String) {
        if (!_isMultiMode.value) {
            val draw = _selectedDraw.value
            if (draw != null && !draw.hasModality(mod)) {
                showSnackbar("El sorteo ${draw.name} no permite la modalidad $mod")
                return
            }
        } else {
            val selectedDraws = _activeDraws.value.filter { _selectedMultiDrawIds.value.contains(it.id) }
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
"""

content = re.sub(r'fun selectModality\(mod: String\) \{.*?updateUnitPrice\(\)\s*\}', new_func.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'w') as f:
    f.write(content)
