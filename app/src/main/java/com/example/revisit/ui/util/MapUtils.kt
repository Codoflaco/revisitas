package com.example.revisit.ui.util // O el paquete que hayas elegido

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color // Importa androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun createMarkerWithLabelBitmap(
    context: Context,
    name: String,
    lastName: String,
    @DrawableRes markerIconResId: Int,
    markerTintColor: Color,         // Este es androidx.compose.ui.graphics.Color
    labelTextColor: Int,            // Este es un Android Color Int (ej. AndroidColor.BLACK)
    labelBackgroundColor: Int,      // Este es un Android Color Int (ej. AndroidColor.WHITE)
    labelPadding: Int = 16,
    labelCornerRadius: Float = 20f,
    iconWidth: Int = 80,
    iconHeight: Int = 80,
    fontSizeSp: Float = 14f,
    labelOffsetY: Int = 10
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val scaledFontSize = fontSizeSp * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelTextColor
        textSize = scaledFontSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val fullName = "$name ${lastName.firstOrNull()?.let { "$it." } ?: ""}".trim()
    val textBounds = Rect()
    textPaint.getTextBounds(fullName, 0, fullName.length, textBounds)

    val labelContentWidth = textBounds.width()
    val labelContentHeight = textBounds.height()

    val labelWidth = labelContentWidth + (2 * labelPadding)
    val labelHeight = labelContentHeight + (2 * labelPadding)

    val totalBitmapWidth = labelWidth.coerceAtLeast(iconWidth)
    val totalBitmapHeight = labelHeight + iconHeight - labelOffsetY

    val finalBitmap = Bitmap.createBitmap(totalBitmapWidth, totalBitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelBackgroundColor
        style = Paint.Style.FILL
    }
    val labelLeft = (totalBitmapWidth - labelWidth) / 2f
    val labelTop = 0f
    canvas.drawRoundRect(
        labelLeft,
        labelTop,
        labelLeft + labelWidth,
        labelTop + labelHeight,
        labelCornerRadius,
        labelCornerRadius,
        backgroundPaint
    )

    val textX = labelLeft + labelWidth / 2f
    val textBaselineY = labelTop + (labelHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(fullName, textX, textBaselineY, textPaint)

    val markerIconDrawable = ContextCompat.getDrawable(context, markerIconResId)
    markerIconDrawable?.let {
        it.setTint(markerTintColor.toArgb()) // markerTintColor es Compose Color, toArgb() lo convierte a Int
        val iconActualX = (totalBitmapWidth - iconWidth) / 2f
        val iconActualY = (labelHeight - labelOffsetY).toFloat()
        it.setBounds(
            iconActualX.toInt(),
            iconActualY.toInt(),
            (iconActualX + iconWidth).toInt(),
            (iconActualY + iconHeight).toInt()
        )
        it.draw(canvas)
    }

    return BitmapDescriptorFactory.fromBitmap(finalBitmap)
}
    