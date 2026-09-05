#!/bin/bash
# Remove first line
sed -i '1d' app/src/main/java/com/example/ui/screens/sales/SalesScreen.kt

# Prepend correct lines
sed -i '1i package com.example.ui.screens.sales\n\nimport androidx.compose.foundation.border\nimport androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background\n' app/src/main/java/com/example/ui/screens/sales/SalesScreen.kt
