package com.ivieleague.wardtextsender

import android.graphics.Color
import android.graphics.Typeface
import android.widget.EditText
import android.widget.TextView
import com.lightningkite.kotlin.anko.textColorResource
import org.jetbrains.anko.textColor

fun EditText.styleDefault() {
    textColor = Color.BLACK
}

fun TextView.styleDefault() {
    textColor = Color.BLACK
}

fun TextView.styleHeader() {
    textSize = 16f
    setTypeface(null, Typeface.BOLD)
    textColorResource = android.R.color.primary_text_dark
}