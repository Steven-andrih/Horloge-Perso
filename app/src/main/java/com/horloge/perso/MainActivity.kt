package com.horloge.perso

import android.app.Activity
import android.app.Dialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var ui: Ui
    private lateinit var preview: PreviewView
    private lateinit var fonts: List<Fonts.Font>
    private var previewHome = false

    inner class PreviewView(ctx: android.content.Context) : View(ctx) {
        init {
            if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) {
                    o.setRoundRect(0, 0, v.width, v.height, ui.dp(22).toFloat())
                }
            }
            clipToOutline = true
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val show = when (prefs.mode) {
                1 -> !previewHome
                2 -> previewHome
                else -> true
            }
            ClockRenderer.draw(canvas, width, height, prefs, show, !previewHome)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.get(this)
        ui = Ui(this)
        fonts = Fonts.list(this)
        preview = PreviewView(this)

        window.statusBarColor = ui.cBg
        window.navigationBarColor = ui.cBg
        window.setBackgroundDrawable(ColorDrawable(ui.cBg))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(16), ui.dp(24), ui.dp(16), ui.dp(120))
        }

        content.addView(ui.tv("Horloge Perso", 30f, bold = true), ui.lp())
        content.addView(
            ui.tv("Personnalise l'écran de ton téléphone", 14f, ui.cSub),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = ui.dp(2); bottomMargin = ui.dp(20) }
        )

        // Aperçu
        ui.section(content, "Aperçu") {
            addView(preview, LinearLayout.LayoutParams(ui.dp(190), ui.dp(410)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = ui.dp(14)
            })
            addView(
                ui.chips(listOf("Verrouillage", "Accueil"), { if (previewHome) 1 else 0 }) {
                    previewHome = it == 1; preview.invalidate()
                }, ui.lp(top = 16)
            )
        }

        profilesSection(content)

        // Style
        ui.section(content, "Style de l'horloge") {
            addView(
                ui.chips(ClockRenderer.styleNames, { prefs.clockStyle }) {
                    prefs.clockStyle = it; preview.invalidate()
                }, ui.lp(top = 14)
            )
        }

        // Police
        ui.section(content, "Police") {
            addView(
                ui.chips(
                    fonts.map { it.label },
                    { fonts.indexOfFirst { f -> f.key == prefs.fontKey }.coerceAtLeast(0) },
                    { prefs.fontKey = fonts[it].key; preview.invalidate() },
                    fonts.map { Fonts.typeface(this@MainActivity, it.key) }
                ), ui.lp(top = 14)
            )
        }

        // Couleurs
        ui.section(content, "Couleurs") {
            addView(ui.label("Couleur de l'heure"))
            addView(ui.colors(ClockRenderer.textColors, { prefs.textColor },
                { prefs.textColor = it; preview.invalidate() }) { apply -> pickColor(prefs.textColor, apply) })
            addView(ui.label("Deuxième couleur (dégradé, aiguille des minutes)"))
            addView(ui.colors(ClockRenderer.textColors, { prefs.color2 },
                { prefs.color2 = it; preview.invalidate() }) { apply -> pickColor(prefs.color2, apply) })
            addView(ui.slider("Opacité de l'horloge", 10, 100, prefs.opacity, "%") {
                prefs.opacity = it; preview.invalidate()
            })
        }

        // Taille, position, options
        ui.section(content, "Taille et position") {
            addView(ui.slider("Taille", 10, 45, prefs.size, "") { prefs.size = it; preview.invalidate() })
            addView(ui.slider("Position verticale", 5, 90, prefs.posY, "%") { prefs.posY = it; preview.invalidate() })
            addView(ui.label("Format de la date"))
            addView(ui.chips(ClockRenderer.dateFormatNames, { prefs.dateFormat }) {
                prefs.dateFormat = it; preview.invalidate()
            })
            addView(ui.switchRow("Format 24 heures", prefs.is24h) { prefs.is24h = it; preview.invalidate() })
            addView(ui.switchRow("Afficher la date", prefs.showDate) { prefs.showDate = it; preview.invalidate() })
            addView(ui.switchRow("Ombre sous le texte", prefs.shadow) { prefs.shadow = it; preview.invalidate() })
        }

        // Où afficher l'horloge
        ui.section(content, "Afficher l'horloge sur") {
            addView(
                ui.chips(ClockRenderer.modeNames, { prefs.mode }) { prefs.mode = it; preview.invalidate() },
                ui.lp(top = 14)
            )
            addView(
                ui.tv(
                    "Dans la fenêtre du système, choisis l'option qui inclut l'écran de verrouillage ET l'accueil. " +
                        "Ce réglage décide ensuite où l'horloge apparaît.", 12f, ui.cSub
                ), ui.lp(top = 12)
            )
        }

        // Fond
        ui.section(content, "Fond d'écran") {
            addView(ui.label("Photo (verrouillage, et accueil si pas d'autre photo)"))
            addView(buttonPair("Choisir", "Retirer", { pick(1) }) { prefs.clearBackground(false); preview.invalidate() })
            addView(ui.label("Photo différente pour l'accueil (optionnel)"))
            addView(buttonPair("Choisir", "Retirer", { pick(2) }) { prefs.clearBackground(true); preview.invalidate() })
            addView(ui.slider("Assombrir la photo", 0, 80, prefs.dim, "%") { prefs.dim = it; preview.invalidate() })
            addView(ui.label("Couleur du fond (sans photo)"))
            addView(ui.colors(ClockRenderer.bgColors, { prefs.bgColor },
                { prefs.bgColor = it; preview.invalidate() }) { apply -> pickColor(prefs.bgColor, apply) })
        }

        // Réinitialiser
        ui.section(content, "Réinitialiser") {
            addView(
                ui.tv(
                    "Remet les réglages d'horloge aux valeurs d'origine. Tes photos et tes profils sont conservés.",
                    13f, ui.cSub
                ), ui.lp(top = 6)
            )
            addView(ui.button("Réinitialiser les réglages", false) {
                confirm(
                    "Réinitialiser ?", "Tous les réglages d'horloge reviendront aux valeurs d'origine.",
                    "Réinitialiser"
                ) {
                    prefs.resetSettings()
                    recreate()
                }
            }, ui.lp(top = 14))
        }

        // Barre d'action fixe en bas
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(16), ui.dp(28), ui.dp(16), ui.dp(16))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00222831, 0xFF222831.toInt())
            )
        }
        bar.addView(ui.button("Appliquer comme fond d'écran", true) { applyWallpaper() }, ui.lp())

        val root = FrameLayout(this)
        root.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }, FrameLayout.LayoutParams(-1, -1))
        root.addView(bar, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)
    }

    private fun profilesSection(parent: LinearLayout) {
        val profiles = Profiles(prefs)
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun refresh() {
            listBox.removeAllViews()
            val all = profiles.list()
            if (all.isEmpty()) {
                listBox.addView(ui.tv("Aucun profil enregistré pour l'instant.", 13f, ui.cSub), ui.lp(top = 14))
            }
            all.forEach { p ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = ui.rounded(ui.cBg, 18)
                    setPadding(ui.dp(16), ui.dp(10), ui.dp(10), ui.dp(10))
                }
                row.addView(ui.tv(p.name, 15f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(ui.smallButton("Charger", true) {
                    profiles.load(p)
                    Toast.makeText(this, "Profil « ${p.name} » chargé", Toast.LENGTH_SHORT).show()
                    recreate()
                }, LinearLayout.LayoutParams(-2, -2))
                row.addView(ui.smallButton("Supprimer", false) {
                    confirm("Supprimer ce profil ?", "« ${p.name} » sera effacé définitivement.", "Supprimer") {
                        profiles.delete(p)
                        refresh()
                    }
                }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = ui.dp(8) })
                listBox.addView(row, ui.lp(top = 10))
            }
        }

        ui.section(parent, "Profils") {
            addView(
                ui.tv(
                    "Enregistre tes réglages et tes photos pour les retrouver en un tap (ex. Travail, Nuit).",
                    13f, ui.cSub
                ), ui.lp(top = 6)
            )
            val input = ui.input("Nom du profil")
            addView(input, ui.lp(top = 14))
            addView(ui.button("Enregistrer le profil actuel", true) {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Donne un nom au profil", Toast.LENGTH_SHORT).show()
                } else {
                    profiles.save(name)
                    input.setText("")
                    refresh()
                    Toast.makeText(this@MainActivity, "Profil « $name » enregistré", Toast.LENGTH_SHORT).show()
                }
            }, ui.lp(top = 10))
            addView(listBox)
            refresh()
        }
    }

    private fun confirm(title: String, message: String, okLabel: String, onOk: () -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(20))
            background = ui.rounded(ui.cCard, 24)
        }
        box.addView(ui.tv(title, 18f, bold = true))
        box.addView(ui.tv(message, 14f, ui.cSub), ui.lp(top = 10))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(ui.button("Annuler", false) { dialog.dismiss() },
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = ui.dp(8) })
        row.addView(ui.button(okLabel, true) { dialog.dismiss(); onOk() }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(row, ui.lp(top = 20))
        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun buttonPair(a: String, b: String, onA: () -> Unit, onB: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(ui.button(a, false) { onA() },
                LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = ui.dp(8) })
            addView(ui.button(b, false) { onB() }, LinearLayout.LayoutParams(0, -2, 1f))
        }

    private fun pick(code: Int) {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), code
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null && prefs.setBackground(uri, requestCode == 2)) {
                preview.invalidate()
            } else {
                Toast.makeText(this, "Impossible de charger cette image", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Sélecteur de couleur libre : teinte, saturation, luminosité. */
    private fun pickColor(initial: Int, onPick: (Int) -> Unit) {
        val hsv = FloatArray(3)
        Color.colorToHSV(initial, hsv)
        val swatch = View(this)
        fun refresh() { swatch.background = ui.rounded(Color.HSVToColor(hsv), 16) }
        refresh()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(20))
            background = ui.rounded(ui.cCard, 24)
        }
        box.addView(ui.tv("Couleur personnalisée", 18f, bold = true))
        box.addView(swatch, LinearLayout.LayoutParams(-1, ui.dp(56)).apply { topMargin = ui.dp(14) })

        fun slider(name: String, max: Int, start: Int, set: (Int) -> Unit) {
            box.addView(ui.label(name))
            box.addView(ui.seekBar(max, start) { set(it); refresh() })
        }
        slider("Teinte", 360, hsv[0].toInt()) { hsv[0] = it.toFloat() }
        slider("Saturation", 100, (hsv[1] * 100).toInt()) { hsv[1] = it / 100f }
        slider("Luminosité", 100, (hsv[2] * 100).toInt()) { hsv[2] = it / 100f }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(ui.button("Annuler", false) { dialog.dismiss() },
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = ui.dp(8) })
        row.addView(ui.button("OK", true) {
            onPick(Color.HSVToColor(hsv)); dialog.dismiss()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(row, ui.lp(top = 20))

        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun applyWallpaper() {
        val comp = ComponentName(this, ClockWallpaperService::class.java)
        try {
            startActivity(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, comp)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Choisis « Horloge Perso » dans la liste", Toast.LENGTH_LONG).show()
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
