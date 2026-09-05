val activeDraws = listOf(
    Pair("CHANCE, PALE", "1"),
    Pair("CHANCE", "2")
)
val selectedMultiIds = setOf("1", "2")
val selectedDraws = activeDraws.filter { selectedMultiIds.contains(it.second) }
val allowedModalities = if (selectedDraws.isNotEmpty()) {
    val sets = selectedDraws.map { it.first.split(",").map { m -> m.trim().uppercase() }.filter { m -> m.isNotEmpty() }.toSet() }
    sets.reduce { acc, set -> acc.intersect(set) }.joinToString(",")
} else {
    ""
}
println(allowedModalities)
