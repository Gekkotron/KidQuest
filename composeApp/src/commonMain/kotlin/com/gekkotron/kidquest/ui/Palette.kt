package com.gekkotron.kidquest.ui

object Palette {
    val childColors: List<Long> = listOf(
        0xFFFF1744, 0xFFFF6D00, 0xFFFFC400, 0xFF76FF03, 0xFF00E676, 0xFF1DE9B6,
        0xFF00B0FF, 0xFF2979FF, 0xFF651FFF, 0xFFD500F9, 0xFFF50057, 0xFFFF4081,
    )
    val avatars: List<String> = (
        listOf("🦄", "🦖", "🦕", "🐉", "🧚", "🐬", "🦋", "🐱", "🐶", "🐧") +
            listOf("🦊", "🐻", "🐼", "🐯", "🦁", "🐸", "🐵", "🐰", "🐲", "🐙", "🚀", "⚽", "🎨", "🌟")
        ).distinct()
    val goalIcons: List<String> = listOf(
        "🎁", "🎳", "🎠", "🍔", "🧱", "🎬", "🍦", "🍕", "🎮", "📱", "🚲", "🏊", "🎢", "📚", "🧸",
        "🎡", "🍟", "🏰", "🎪", "🍿", "🛝", "⛸️",
    ).distinct()
    val actionIcons: List<String> = listOf("⭐", "🧽", "👟", "🧹", "🍽️", "🪥", "🛏️", "🧺", "🐱", "📚", "🌱", "😠", "🙉", "📵")
}
