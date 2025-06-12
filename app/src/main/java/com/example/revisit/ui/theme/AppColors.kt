package com.example.revisit.ui.theme // o ui.util

import androidx.compose.ui.graphics.Color

object VisitStatusAppColors {
    // Opción A: Si los colores son fijos y no dependen del tema claro/oscuro
    val Today: Color = Color(0xFFFFA500) // Naranja
    val Overdue: Color = Color.Red // O Color(0xFFFF0000)
    val DueSoon: Color = Color.Yellow // Amarillo
    val DueFar: Color = Color.Green
    val ColorNoDate: Color = Color.Gray // Un azul, por ejemplo
}