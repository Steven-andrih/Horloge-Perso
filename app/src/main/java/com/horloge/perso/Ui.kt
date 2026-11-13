package com.horloge.perso

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Conteneur qui passe à la ligne comme du texte (pour les pastilles). */
class FlowLayout(ctx: Context, private val hGap: Int, private val vGap: Int) : ViewGroup(ctx) {

    private fun childSpecs(c: View, maxW: Int): Pair<Int, Int> {
        val lp = c.layoutParams
        val ws = if (lp != null && lp.width > 0) MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
        else MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST)
        val hs = if (lp != null && lp.height > 0) MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
        else MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        return ws to hs
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val maxW = MeasureSpec.getSize(wSpec) - paddingLeft - paddingRight
        var x = 0; var y = 0; var rowH = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            val (ws, hs) = childSpecs(c, maxW)
            c.measure(ws, hs)
            val cw = c.measuredWidth
            if (x > 0 && x + cw > maxW) { x = 0; y += rowH + vGap; rowH = 0 }
            x += cw + hGap
            rowH = maxOf(rowH, c.measuredHeight)
        }
        setMeasuredDimension(MeasureSpec.getSize(wSpec), y + rowH + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = r - l - paddingLeft - paddingRight
        var x = 0; var y = 0; var rowH = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            val cw = c.measuredWidth
            val ch = c.measuredHeight
            if (x > 0 && x + cw > maxW) { x = 0; y += rowH + vGap; rowH = 0 }
            c.layout(paddingLeft + x, paddingTop + y, paddingLeft + x + cw, paddingTop + y + ch)
            x += cw + hGap
            rowH = maxOf(rowH, ch)
        }
    }
}

/** Palette et composants façon Spotify : fond sombre, cartes arrondies, accent vert. */
class Ui(val ctx: Context) {
    val cBg = 0xFF222831.toInt()
    val cCard = 0xFF393E46.toInt()
    val cAccent = 0xFF7BC74D.toInt()
    val cText = 0xFFEEEEEE.toInt()
    val cSub = 0xFFA9B0B8.toInt()

    val regularFace: Typeface = Fonts.assetOrNull(ctx, "Poppins-Medium.ttf") ?: Typeface.DEFAULT
    val boldFace: Typeface = Fonts.assetOrNull(ctx, "Poppins-Bold.ttf") ?: Typeface.DEFAULT_BOLD

    fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun ripple(d: Drawable): Drawable = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), d, null)

    fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    fun tv(t: String, sp: Float, color: Int = cText, bold: Boolean = false) = TextView(ctx).apply {
        text = t
        textSize = sp
        setTextColor(color)
        typeface = if (bold) boldFace else regularFace
    }

    fun label(t: String) = tv(t, 13f, cSub).apply { setPadding(0, dp(16), 0, dp(8)) }

    fun section(parent: LinearLayout, title: String, build: LinearLayout.() -> Unit) {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            background = rounded(cCard, 24)
        }
        box.addView(tv(title, 18f, bold = true))
        box.build()
        parent.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
    }

    fun button(label: String, primary: Boolean, onClick: () -> Unit) = TextView(ctx).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 15f
        typeface = boldFace
        setTextColor(if (primary) cBg else cText)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = ripple(rounded(if (primary) cAccent else cBg, 28))
        setOnClickListener { onClick() }
    }

    fun smallButton(label: String, primary: Boolean, onClick: () -> Unit) = TextView(ctx).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 13f
        typeface = boldFace
        setTextColor(if (primary) cBg else cText)
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = ripple(rounded(if (primary) cAccent else cCard, 18))
        setOnClickListener { onClick() }
    }

    fun input(hintText: String) = EditText(ctx).apply {
        hint = hintText
        setHintTextColor(cSub)
        setTextColor(cText)
        textSize = 14f
        typeface = regularFace
        setSingleLine(true)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = rounded(cBg, 22)
    }

    /** Pastilles à choix unique. */
    fun chips(options: List<String>, selected: () -> Int, onSelect: (Int) -> Unit): FlowLayout =
        chips(options, selected, onSelect, null)

    fun chips(
        options: List<String>, selected: () -> Int, onSelect: (Int) -> Unit, faces: List<Typeface>?
    ): FlowLayout {
        val flow = FlowLayout(ctx, dp(8), dp(8))
        fun build() {
            flow.removeAllViews()
            options.forEachIndexed { i, name ->
                val sel = i == selected()
                flow.addView(TextView(ctx).apply {
                    text = name
                    textSize = 14f
                    typeface = faces?.getOrNull(i) ?: regularFace
                    setTextColor(if (sel) cBg else cText)
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    background = ripple(rounded(if (sel) cAccent else cBg, 22))
                    setOnClickListener { onSelect(i); build() }
                }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        build()
        return flow
    }

    private fun dot(color: Int, selected: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (selected) setStroke(dp(3), cAccent) else setStroke(dp(1), 0x55EEEEEE)
    }

    /** Pastilles de couleur + bouton « + » pour une couleur libre. */
    fun colors(
        list: IntArray, current: () -> Int, onPick: (Int) -> Unit, custom: ((Int) -> Unit) -> Unit
    ): FlowLayout {
        val flow = FlowLayout(ctx, dp(10), dp(10))
        fun build() {
            flow.removeAllViews()
            val cur = current()
            list.forEach { col ->
                flow.addView(View(ctx).apply {
                    background = dot(col, col == cur)
                    setOnClickListener { onPick(col); build() }
                }, ViewGroup.LayoutParams(dp(40), dp(40)))
            }
            val isCustom = list.none { it == cur }
            flow.addView(TextView(ctx).apply {
                text = if (isCustom) "" else "+"
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(cText)
                background = dot(if (isCustom) cur else cBg, isCustom)
                setOnClickListener { custom { col -> onPick(col); build() } }
            }, ViewGroup.LayoutParams(dp(40), dp(40)))
        }
        build()
        return flow
    }

    fun seekBar(max: Int, progress: Int, onChange: (Int) -> Unit) = SeekBar(ctx).apply {
        this.max = max
        this.progress = progress
        progressTintList = ColorStateList.valueOf(cAccent)
        progressBackgroundTintList = ColorStateList.valueOf(0x55EEEEEE)
        thumbTintList = ColorStateList.valueOf(cAccent)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) onChange(p) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    fun slider(title: String, min: Int, max: Int, value: Int, unit: String, onChange: (Int) -> Unit): LinearLayout {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(14), 0, 0) }
        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val valueView = tv("$value$unit", 13f, cAccent, bold = true)
        header.addView(tv(title, 14f), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(valueView)
        box.addView(header)
        box.addView(seekBar(max - min, value - min) {
            valueView.text = "${it + min}$unit"
            onChange(it + min)
        })
        return box
    }

    fun switchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        row.addView(tv(title, 14f), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(ctx).apply {
            isChecked = checked
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(cAccent, cText))
            trackTintList = ColorStateList(states, intArrayOf(0x887BC74D.toInt(), cBg))
            setOnCheckedChangeListener { _, c -> onChange(c) }
        })
        return row
    }
}
