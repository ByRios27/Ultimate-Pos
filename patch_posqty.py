import re

with open('app/src/main/java/com/example/ui/components/PosComponents.kt', 'r') as f:
    content = f.read()

# Fix displayQty
old_qty_logic = """
                        val displayQty = when {
                            isQtyActive -> if (quantityInput.isEmpty()) "--" else quantityInput
                            quantityInput.isNotEmpty() -> quantityInput
                            else -> "$quantity"
                        }
"""
new_qty_logic = """
                        val displayQty = when {
                            isQtyActive && quantityInput.isEmpty() -> "1"
                            quantityInput.isNotEmpty() -> quantityInput
                            else -> if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                        }
"""

content = content.replace(old_qty_logic.strip(), new_qty_logic.strip())

with open('app/src/main/java/com/example/ui/components/PosComponents.kt', 'w') as f:
    f.write(content)
