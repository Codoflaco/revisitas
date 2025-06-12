package com.example.revisit.ui.theme // o ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object VisitStatusAppColors {
    // Opción A: Si los colores son fijos y no dependen del tema claro/oscuro
    val Today: Color = Color(0xFFFFA500) // Naranja
    val Overdue: Color = Color.Red // O Color(0xFFFF0000)
    val DueSoon: Color = Color.Yellow // Amarillo
    val DueFar: Color = Color.Green
    val ColorNoDate: Color = Color.Gray // Un azul, por ejemplo

    // Opción B: Si los colores DEBEN provenir del tema actual (MaterialTheme)
    // Estos necesitarían ser accedidos desde un @Composable
    val overdueFromTheme: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val dueSoonFromTheme: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary // O el que uses

    val scheduledFromTheme: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary // O el que uses
}