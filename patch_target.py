import re

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'r') as f:
    content = f.read()

# Fix targetDigits for auto-switch
old_target = """
                val targetDigits = if (maxAllowed < 2) maxAllowed else 2
                if (newNumber.length == targetDigits) {
"""
new_target = """
                if (newNumber.length == maxAllowed) {
"""
content = content.replace(old_target.strip(), new_target.strip())

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'w') as f:
    f.write(content)
