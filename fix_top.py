with open('app/src/main/java/com/example/ui/screens/sales/SalesScreen.kt', 'r') as f:
    lines = f.readlines()
# Find the first line that is import androidx.compose.foundation.layout.*
start_idx = 0
for i, line in enumerate(lines):
    if 'import androidx.compose.foundation.layout.*' in line:
        start_idx = i
        break

new_lines = [
    'package com.example.ui.screens.sales\n',
    '\n',
    'import androidx.compose.foundation.border\n',
    'import androidx.compose.foundation.BorderStroke\n',
    'import androidx.compose.foundation.background\n'
] + lines[start_idx:]

with open('app/src/main/java/com/example/ui/screens/sales/SalesScreen.kt', 'w') as f:
    f.writelines(new_lines)
