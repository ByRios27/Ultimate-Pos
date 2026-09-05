sed -i 's/p1_full.isNotBlank() && num == p1_full -> item.quantity \* mult1/p1.isNotBlank() \&\& num == p1 -> item.quantity * mult1/g' app/src/main/java/com/example/util/PrizeCalculator.kt
sed -i 's/p1_full.isNotBlank() && num == p1_full -> "1er Premio/p1.isNotBlank() \&\& num == p1 -> "1er Premio/g' app/src/main/java/com/example/util/PrizeCalculator.kt
