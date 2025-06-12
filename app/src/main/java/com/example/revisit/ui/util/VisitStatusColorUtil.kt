package com.example.revisit.ui.util

import androidx.compose.runtime.Composable // Ver nota abajo sobre @Composable
import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.concurrent.TimeUnit // Para convertir milisegundos a días
import com.example.revisit.ui.theme.VisitStatusAppColors // Asegúrate que esta es la ruta correcta

object VisitStatusColorUtil {

    // Estas constantes locales podrían quedar obsoletas si VisitStatusAppColors las cubre todas
    val ColorPast: Color = Color.Red
    val ColorToday: Color = Color(0xFFFFA500) // Naranja
    val ColorApproaching: Color = Color.Yellow
    val ColorFar: Color = Color.Green
    val ColorNoDate: Color = Color.Gray

    // Este ya no se usa con la nueva lógica
    // const val PERCENTAGE_THRESHOLD_FOR_APPROACHING: Double = 0.50 // 50%

    // Si VisitStatusAppColors NO usa MaterialTheme.colorScheme para definir sus colores,
    // esta función NO necesita ser @Composable. Si SÍ los usa, entonces esta función
    // y la función que la llame, SÍ necesitan ser @Composable.
    // Por ahora, la dejaré como @Composable por si acaso, pero revísalo.
    @Composable
    fun getVisitStatusColor(
        nextVisitTimestamp: Long?,
        referenceStartDateForNextVisit: Long? // Este parámetro ya no se usa con la nueva lógica
    ): Color {
        if (nextVisitTimestamp == null || nextVisitTimestamp == 0L) {
            return ColorNoDate // Usando tu constante local para sin fecha
        }

        val today = Calendar.getInstance()
        // Normalizar 'today' al inicio del día para comparaciones consistentes
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val nextVisitDate = Calendar.getInstance()
        nextVisitDate.timeInMillis = nextVisitTimestamp
        // Normalizar 'nextVisitDate' al inicio del día
        nextVisitDate.set(Calendar.HOUR_OF_DAY, 0)
        nextVisitDate.set(Calendar.MINUTE, 0)
        nextVisitDate.set(Calendar.SECOND, 0)
        nextVisitDate.set(Calendar.MILLISECOND, 0)

        // --- Lógica de Días de Diferencia ---
        // Diferencia en milisegundos
        val diffMillis = nextVisitDate.timeInMillis - today.timeInMillis
        // Convertir la diferencia a días.
        // TimeUnit.MILLISECONDS.toDays() trunca, así que es bueno para contar días completos.
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        // --- Determinar el Color según los nuevos requisitos ---
        return when {
            // Caso 1: Anterior a la fecha actual (Rojo)
            // nextVisitDate es antes que today (ambos normalizados al inicio del día)
            // Esto también se puede ver como diffDays < 0
            nextVisitDate.before(today) -> {
                // Asumo que tienes VisitStatusAppColors.Overdue para el rojo
                VisitStatusAppColors.Overdue
                // o si prefieres tu constante local: ColorPast
            }

            // Caso 2: El mismo día (Naranja)
            // diffDays será 0 si son el mismo día después de la normalización
            diffDays == 0L -> {
                // Asumo que tienes VisitStatusAppColors.Today para el naranja
                VisitStatusAppColors.Today
                // o si prefieres tu constante local: ColorToday
            }

            // Caso 3: Entre 1 y 3 días (Amarillo)
            // diffDays será 1 (mañana), 2, o 3.
            diffDays in 1..3 -> {
                // Para Amarillo, podrías usar ColorApproaching o un VisitStatusAppColors.DueSoon
                // Si VisitStatusAppColors.DueSoon es tu amarillo:
                VisitStatusAppColors.DueSoon
                // o si prefieres tu constante local: ColorApproaching
            }

            // Caso 4: Mayor que 3 días (Verde)
            // Originalmente dijiste "mayor que 4 dias", lo cual significaría diffDays >= 5.
            // Si es "mayor que 3 días" (es decir, 4 días en adelante), entonces diffDays >= 4.
            // Voy a asumir "4 días en adelante" basado en tu corrección que faltaba el rango de 1 a 3.
            // Si "mayor que 4 días" significa que a partir del 5to día es verde, entonces sería diffDays > 4
            // Si significa que a partir del 4to día (inclusive) es verde, entonces diffDays > 3
            diffDays > 3 -> { // Es decir, 4 días o más en el futuro
                // Para Verde, podrías usar ColorFar o un VisitStatusAppColors.DueFar
                VisitStatusAppColors.DueFar
                // o si prefieres tu constante local: ColorFar
            }

            // Fallback: esto podría cubrir casos inesperados o si nextVisitTimestamp es
            // en el pasado pero no capturado por nextVisitDate.before(today) por alguna razón
            // (aunque no debería con la normalización). O si diffDays es negativo
            // y no entró en el primer caso (de nuevo, improbable).
            // Si diffDays es negativo, ya debería haber entrado en el primer caso.
            else -> ColorNoDate // Por defecto, si ninguna condición anterior se cumple
        }
    }
}