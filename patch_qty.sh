sed -i 's/MutableStateFlow(1)/MutableStateFlow(1.0)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/StateFlow<Int>/StateFlow<Double>/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/currentQuantity.value = 1/currentQuantity.value = 1.0/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/toIntOrNull()/toDoubleOrNull()/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/coerceIn(1, 9999)/coerceIn(0.01, 9999.0)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
