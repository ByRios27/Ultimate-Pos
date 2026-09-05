sed -i 's/fun setQuantity(qty: Int)/fun setQuantity(qty: Double)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/fun addQuickQuantity(increment: Int)/fun addQuickQuantity(increment: Double)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/fun multiplyQuantity(multiplier: Int)/fun multiplyQuantity(multiplier: Double)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/fun updateCartItemQuantity(itemId: String, newQuantity: Int)/fun updateCartItemQuantity(itemId: String, newQuantity: Double)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/fun updateCartItem(itemId: String, newNumber: String, newQuantity: Int/fun updateCartItem(itemId: String, newNumber: String, newQuantity: Double/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
sed -i 's/fun updateSaleItem(item: SaleItem, newNumber: String, newQuantity: Int)/fun updateSaleItem(item: SaleItem, newNumber: String, newQuantity: Double)/g' app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt
