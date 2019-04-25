/*
 * Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.arcgisruntime.toolkit.scalebar.style.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.esri.arcgisruntime.UnitSystem
import com.esri.arcgisruntime.geometry.LinearUnit
import com.esri.arcgisruntime.toolkit.extension.dpToPixels
import com.esri.arcgisruntime.toolkit.java.scalebar.ScalebarUtil
import com.esri.arcgisruntime.toolkit.scalebar.LINEAR_UNIT_FEET
import com.esri.arcgisruntime.toolkit.scalebar.LINEAR_UNIT_METERS
import com.esri.arcgisruntime.toolkit.scalebar.SHADOW_OFFSET_PIXELS
import com.esri.arcgisruntime.toolkit.scalebar.style.Style

/**
 * Renders a DUAL_UNIT_LINE style scalebar.
 *
 * @see Style.DUAL_UNIT_LINE
 *
 * @since 100.2.1
 */
class DualUnitLineRenderer : ScalebarRenderer() {

    override val isSegmented: Boolean = false

    override fun drawScalebar(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        distance: Double,
        displayUnits: LinearUnit,
        unitSystem: UnitSystem,
        lineWidthDp: Int,
        cornerRadiusDp: Int,
        textSizeDp: Int,
        fillColor: Int,
        alternateFillColor: Int,
        shadowColor: Int,
        lineColor: Int,
        textPaint: Paint,
        displayDensity: Float
    ) {
        // Calculate scalebar length in the secondary units
        val secondaryBaseUnits = if (unitSystem == UnitSystem.METRIC) LINEAR_UNIT_FEET else LINEAR_UNIT_METERS
        val fullLengthInSecondaryUnits = displayUnits.convertTo(secondaryBaseUnits, distance)

        // Reduce the secondary units length to make it a nice number
        var secondaryUnitsLength =
            ScalebarUtil.calculateBestScalebarLength(fullLengthInSecondaryUnits, secondaryBaseUnits, false)
        val lineDisplayLength = right - left
        val xPosSecondaryTick =
            left + (lineDisplayLength * secondaryUnitsLength / fullLengthInSecondaryUnits).toFloat()

        // Change units if secondaryUnitsLength is too big a number in the base units
        val secondaryUnitSystem = if (unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
        val secondaryDisplayUnits = ScalebarUtil.selectLinearUnit(secondaryUnitsLength, secondaryUnitSystem)
        if (secondaryDisplayUnits != secondaryBaseUnits) {
            secondaryUnitsLength = secondaryBaseUnits.convertTo(secondaryDisplayUnits, secondaryUnitsLength)
        }

        // Create Paint for drawing the lines
        paint.reset()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = lineWidthDp.dpToPixels(displayDensity).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        // Create a path to draw the line and the ticks
        val yPosLine = (top + bottom) / 2
        linePath.reset()
        linePath.moveTo(left, top)
        linePath.lineTo(left, bottom) // draw big tick at left
        linePath.moveTo(xPosSecondaryTick, yPosLine) // move to top of secondary tick
        linePath.lineTo(xPosSecondaryTick, bottom) // draw secondary tick
        linePath.moveTo(left, yPosLine) // move to start of horizontal line
        linePath.lineTo(right, yPosLine) // draw the line
        linePath.lineTo(right, top) // draw tick at right
        linePath.setLastPoint(right, top)

        // Create a copy of the line path to be the path of its shadow, offset slightly from the line path
        val shadowPath = Path(linePath)
        shadowPath.offset(SHADOW_OFFSET_PIXELS, SHADOW_OFFSET_PIXELS)

        // Draw the shadow
        paint.color = shadowColor
        canvas.drawPath(shadowPath, paint)

        // Draw the line and the ticks
        paint.color = lineColor
        canvas.drawPath(linePath, paint)

        // Draw the primary units label above the tick at the right hand end
        val maxPixelsBelowBaseline = textPaint.fontMetrics.bottom
        var yPosText = top - maxPixelsBelowBaseline
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(ScalebarUtil.labelString(distance), right, yPosText, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(' ' + displayUnits.abbreviation, right, yPosText, textPaint)

        // Draw the secondary units label below its tick
        yPosText = bottom + textSizeDp.dpToPixels(displayDensity)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(ScalebarUtil.labelString(secondaryUnitsLength), xPosSecondaryTick, yPosText, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(' ' + secondaryDisplayUnits.abbreviation, xPosSecondaryTick, yPosText, textPaint)
    }

    override fun calculateExtraSpaceForUnits(displayUnits: LinearUnit?, textPaint: Paint): Float {
        return calculateWidthOfUnitsString(displayUnits, textPaint)
    }

}
