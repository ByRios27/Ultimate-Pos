import re

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'r') as f:
    content = f.read()

# Add validation in addToCart for CHANCE digits
old_add = """
        if (_selectedModality.value == "PALE") {
            val clean = num.replace("-", "")
            if (clean.length < 2) {
                showSnackbar("Ingresa los números completos para el Palé (ej. 25-78)")
                return
            }
        }
"""
new_add = """
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
"""
content = content.replace(old_add.strip(), new_add.strip())

# Block dot for CHANCE in appendDigitToQuantity
old_qty_append = """
        // Prevent multiple dots
        if (digit == "." && currentStr.contains(".")) return
"""
new_qty_append = """
        // Prevent multiple dots, and prevent dots entirely for CHANCE
        if (digit == ".") {
            if (_selectedModality.value == "CHANCE") return
            if (currentStr.contains(".")) return
        }
"""
content = content.replace(old_qty_append.strip(), new_qty_append.strip())

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'w') as f:
    f.write(content)
