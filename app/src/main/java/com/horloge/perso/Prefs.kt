package com.horloge.perso

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.File

class Prefs private constructor(val ctx: Context, val sp: SharedPreferences) {
    var fontKey: String
        get() = sp.getString("fontKey", "sys:sans-serif-light") ?: "sys:sans-serif-light"
        set(v) = sp.edit().putString("fontKey", v).apply()
    var textColor: Int
        get() = sp.getInt("textColor", 0xFFFFFFFF.toInt())
        set(v) = sp.edit().putInt("textColor", v).apply()
    var bgColor: Int
        get() = sp.getInt("bgColor", 0xFF101018.toInt())
        set(v) = sp.edit().putInt("bgColor", v).apply()
    var clockStyle: Int   // 0 classique, 1 empilé, 2 contour, 3 dégradé, 4 analogique
        get() = sp.getInt("clockStyle", 0)
        set(v) = sp.edit().putInt("clockStyle", v).apply()
    var color2: Int
        get() = sp.getInt("color2", 0xFF7BC74D.toInt())
        set(v) = sp.edit().putInt("color2", v).apply()
    var dateFormat: Int
        get() = sp.getInt("dateFormat", 0)
        set(v) = sp.edit().putInt("dateFormat", v).apply()
    var opacity: Int   // opacité de l'horloge en %
        get() = sp.getInt("opacity", 100)
        set(v) = sp.edit().putInt("opacity", v).apply()
    var size: Int
        get() = sp.getInt("size", 28)
        set(v) = sp.edit().putInt("size", v).apply()
    var posY: Int
        get() = sp.getInt("posY", 30)
        set(v) = sp.edit().putInt("posY", v).apply()
    var is24h: Boolean
        get() = sp.getBoolean("is24h", true)
        set(v) = sp.edit().putBoolean("is24h", v).apply()
    var showDate: Boolean
        get() = sp.getBoolean("showDate", true)
        set(v) = sp.edit().putBoolean("showDate", v).apply()
    var shadow: Boolean
        get() = sp.getBoolean("shadow", false)
        set(v) = sp.edit().putBoolean("shadow", v).apply()
    /** 0 = accueil + verrouillage, 1 = verrouillage seulement, 2 = accueil seulement */
    var mode: Int
        get() = sp.getInt("mode", 0)
        set(v) = sp.edit().putInt("mode", v).apply()
    var dim: Int
        get() = sp.getInt("dim", 0)
        set(v) = sp.edit().putInt("dim", v).apply()
    var bgVersion: Int
        get() = sp.getInt("bgVersion", 0)
        set(v) = sp.edit().putInt("bgVersion", v).apply()

    private fun file(home: Boolean) = File(ctx.filesDir, if (home) "bg_home.jpg" else "bg.jpg")

    private fun refresh() {
        val v = bgVersion
        if (v != cachedVersion) {
            cachedMain = load(file(false))
            cachedHome = load(file(true))
            cachedVersion = v
        }
    }
    private fun load(f: File): Bitmap? = if (f.exists()) BitmapFactory.decodeFile(f.path) else null

    fun background(): Bitmap? { refresh(); return cachedMain }
    fun homeBackground(): Bitmap? { refresh(); return cachedHome }

    fun setBackground(uri: Uri, home: Boolean): Boolean {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            while (maxOf(opts.outWidth, opts.outHeight) / sample > 1920) sample *= 2
            val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return false
            file(home).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bgVersion = bgVersion + 1
            true
        } catch (e: Exception) { false }
    }

    private val settingKeys = listOf(
        "fontKey", "textColor", "bgColor", "clockStyle", "color2", "dateFormat",
        "opacity", "size", "posY", "is24h", "showDate", "shadow", "mode", "dim"
    )

    fun snapshot(): JSONObject {
        val o = JSONObject()
        val all = sp.all
        for (k in settingKeys) {
            val v = all[k]
            if (v != null) o.put(k, v)
        }
        return o
    }

    fun restore(o: JSONObject) {
        val e = sp.edit()
        settingKeys.forEach { e.remove(it) }
        for (k in settingKeys) {
            if (!o.has(k)) continue
            when (val v = o.get(k)) {
                is Int -> e.putInt(k, v)
                is Long -> e.putInt(k, v.toInt())
                is Boolean -> e.putBoolean(k, v)
                is String -> e.putString(k, v)
            }
        }
        e.apply()
    }

    /** Remet les réglages d'horloge par défaut. Les photos sont conservées. */
    fun resetSettings() {
        val e = sp.edit()
        settingKeys.forEach { e.remove(it) }
        e.apply()
    }

    private fun profileFile(id: Long, home: Boolean) =
        File(ctx.filesDir, "profile_${id}_${if (home) "home" else "main"}.jpg")

    fun saveBackgroundsAs(id: Long) {
        for (home in listOf(false, true)) {
            val src = file(home)
            if (src.exists()) src.copyTo(profileFile(id, home), overwrite = true)
        }
    }

    fun loadBackgroundsFrom(id: Long) {
        for (home in listOf(false, true)) {
            val src = profileFile(id, home)
            if (src.exists()) src.copyTo(file(home), overwrite = true) else file(home).delete()
        }
        bgVersion = bgVersion + 1
    }

    fun deleteProfileFiles(id: Long) {
        for (home in listOf(false, true)) profileFile(id, home).delete()
    }

    fun clearBackground(home: Boolean) {
        file(home).delete()
        bgVersion = bgVersion + 1
    }

    companion object {
        private var cachedVersion = -1
        private var cachedMain: Bitmap? = null
        private var cachedHome: Bitmap? = null
        fun get(c: Context): Prefs {
            val app = c.applicationContext
            return Prefs(app, app.getSharedPreferences("clock", Context.MODE_PRIVATE))
        }
    }
}
