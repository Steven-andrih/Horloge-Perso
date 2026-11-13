package com.horloge.perso

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Profils enregistrés : réglages d'horloge + photos de fond. */
class Profiles(private val prefs: Prefs) {
    data class Profile(val id: Long, val name: String, val settings: JSONObject)

    private val sp = prefs.ctx.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun list(): List<Profile> {
        val out = ArrayList<Profile>()
        try {
            val arr = JSONArray(sp.getString("list", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Profile(o.getLong("id"), o.getString("name"), o.getJSONObject("settings")))
            }
        } catch (e: Exception) {
            // liste illisible : on repart d'une liste vide
        }
        return out
    }

    private fun write(items: List<Profile>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("settings", it.settings))
        }
        sp.edit().putString("list", arr.toString()).apply()
    }

    /** Enregistre l'état actuel. Un profil du même nom est remplacé. */
    fun save(name: String) {
        val clean = name.trim()
        val current = list()
        val old = current.filter { it.name.equals(clean, ignoreCase = true) }
        old.forEach { prefs.deleteProfileFiles(it.id) }
        val id = System.currentTimeMillis()
        prefs.saveBackgroundsAs(id)
        val kept = current.filter { p -> old.none { it.id == p.id } }
        write(kept + Profile(id, clean, prefs.snapshot()))
    }

    fun load(p: Profile) {
        prefs.restore(p.settings)
        prefs.loadBackgroundsFrom(p.id)
    }

    fun delete(p: Profile) {
        prefs.deleteProfileFiles(p.id)
        write(list().filter { it.id != p.id })
    }
}
