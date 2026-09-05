# Fix AppDatabase (just replace quantity = [0-9]+,)
sed -E -i 's/quantity = ([0-9]+),/quantity = \1.0,/g' app/src/main/java/com/example/data/local/AppDatabase.kt

# Fix DashboardScreen totalPieces parameter
sed -i 's/totalPieces: Int/totalPieces: Double/g' app/src/main/java/com/example/ui/screens/dashboard/DashboardScreen.kt

# Fix ArchivosScreen soldQty comparisons
sed -i 's/soldQty > 0/soldQty > 0.0/g' app/src/main/java/com/example/ui/screens/other/ArchivosScreen.kt

# Fix PosViewModel Elvis operator type inference
sed -i 's/?: 1/?: 1.0/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt

# Fix PosComponents (there might be an explicit 1 passed)
sed -E -i 's/onUpdateQuantity\(item.id, 1\)/onUpdateQuantity(item.id, 1.0)/g' app/src/main/java/com/example/ui/components/PosComponents.kt
