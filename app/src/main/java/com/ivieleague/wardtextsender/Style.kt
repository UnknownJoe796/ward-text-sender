package com.ivieleague.wardtextsender

import android.graphics.Color
import android.widget.EditText
import android.widget.TextView
import org.jetbrains.anko.textColor

fun EditText.styleDefault() {
    textColor = Color.BLACK
}

fun TextView.styleDefault() {
    textColor = Color.BLACK
}

fun TextView.styleHeader() {
    textColor = Color.BLACK
    textSize = 22f
}