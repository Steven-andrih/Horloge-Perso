package com.horloge.perso

import android.content.Context
import android.graphics.Typeface

object Fonts {
    data class Font(val label: String, val key: String)

    private val system = listOf(
        "Fin" to "sans-serif-thin", "Léger" to "sans-serif-light", "Normal" to "sans-serif",
        "Condensé" to "sans-serif-condensed", "Épais" to "sans-serif-black",
        "Serif" to "serif", "Mono" to "monospace", "Cursive" to "cursive"
    )

    fun list(ctx: Context): List<Font> {
        val sys = system.map { Font(it.first, "sys:" + it.second) }
        val extra = try {
            ctx.assets.list("fonts")
                ?.filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) }
                ?.sorted()
                ?.map { Font(label(it), "asset:$it") }
        } catch (e: Exception) { null } ?: emptyList()
        return sys + extra
    }

    private fun label(file: String) = file.substringBeforeLast('.')
        .replace("-Regular", "")
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('-', ' ').replace('_', ' ')

    fun assetOrNull(ctx: Context, file: String): Typeface? =
        try { Typeface.createFromAsset(ctx.assets, "fonts/$file") } catch (e: Exception) { null }

    private val cache = HashMap<String, Typeface>()

    fun typeface(ctx: Context, key: String): Typeface = cache.getOrPut(key) {
        if (key.startsWith("asset:")) {
            try { Typeface.createFromAsset(ctx.assets, "fonts/" + key.removePrefix("asset:")) }
            catch (e: Exception) { Typeface.DEFAULT }
        } else {
            Typeface.create(key.removePrefix("sys:"), Typeface.NORMAL)
        }
    }
}
