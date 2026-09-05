fun String.removeEmojis(): String {
    return this.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\s]"), "").trim()
}
println("Lotería Nacional 🎲🇦🇮".removeEmojis())
println("Sorteo 12:00 PM ⏰".removeEmojis())
println("Sorteo \$1.00!".removeEmojis())
