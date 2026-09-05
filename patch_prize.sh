sed -i 's/val p1 = result.firstPrize.trim()/val p1_full = result.firstPrize.trim()\n        val p2_full = result.secondPrize.trim()\n        val p3_full = result.thirdPrize.trim()\n\n        val p1 = if (p1_full.length >= 2) p1_full.takeLast(2) else p1_full\n        val p2 = if (p2_full.length >= 2) p2_full.takeLast(2) else p2_full\n        val p3 = if (p3_full.length >= 2) p3_full.takeLast(2) else p3_full/g' app/src/main/java/com/example/util/PrizeCalculator.kt
sed -i '/val p2 = result.secondPrize.trim()/d' app/src/main/java/com/example/util/PrizeCalculator.kt
sed -i '/val p3 = result.thirdPrize.trim()/d' app/src/main/java/com/example/util/PrizeCalculator.kt
sed -i 's/p1.isNotBlank() && num == p1/p1_full.isNotBlank() \&\& num == p1_full/g' app/src/main/java/com/example/util/PrizeCalculator.kt
sed -i 's/p1_full_full/p1_full/g' app/src/main/java/com/example/util/PrizeCalculator.kt
