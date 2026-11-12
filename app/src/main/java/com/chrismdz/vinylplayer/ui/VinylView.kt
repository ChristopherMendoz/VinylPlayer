package com.chrismdz.vinylplayer.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.hypot

/*
 * Animation logic (unchanged): the label spins one turn every 6 s while playing and stops where it
 * is when paused; the tonearm eases between its rest and play positions in 800 ms. Everything
 * below the composable is drawing only.
 */
@Composable
fun VinylView(
    artwork: Bitmap?,
    isPlaying: Boolean,
    tonearmAnimationEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val tonearmProgress = remember { Animatable(1f) }
    val tonearmOnRecord = isPlaying || !tonearmAnimationEnabled

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 6_000, easing = LinearEasing)
                )
            }
        }
    }

    LaunchedEffect(tonearmOnRecord) {
        tonearmProgress.animateTo(
            targetValue = if (tonearmOnRecord) 1f else 0f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val labelColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Record surface: static drawing. Its grooves are rotationally symmetric and its sheen
        // comes from a fixed light, so it is drawn once and never redrawn while spinning.
        Spacer(Modifier.fillMaxSize().vinylRecordBody())
        // The cover printed across the whole record (as before), under the grooves and the sheen.
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Fit,
                alpha = 0.9f
            )
        }
        Spacer(Modifier.fillMaxSize().vinylRecordSurface())

        // Center label: same size (45 % of the diameter) and the only rotating part, as before.
        Box(
            modifier = Modifier
                .fillMaxWidth(LABEL_FRACTION)
                .aspectRatio(1f)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
                .background(labelColor),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = "Carátula del disco",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Label rim, paper shading and the metal spindle: fixed, lit from the same light source.
        Spacer(
            Modifier
                .fillMaxWidth(LABEL_FRACTION)
                .aspectRatio(1f)
                .labelAndSpindle()
        )

        // Tonearm on top; progress is read while drawing, so its motion doesn't recompose.
        Spacer(Modifier.fillMaxSize().tonearm { tonearmProgress.value })
    }
}

private const val LABEL_FRACTION = 0.45f

// ---------------------------------------------------------------------------------------------
// Record
// ---------------------------------------------------------------------------------------------

/** Black vinyl body and the shadow it casts; the cover and grooves are drawn over it. */
private fun Modifier.vinylRecordBody(): Modifier = drawWithCache {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f
    val body = Brush.radialGradient(
        0f to Color(0xFF1B1B1D),
        0.55f to Color(0xFF0E0E10),
        0.9f to Color(0xFF070708),
        1f to Color(0xFF030303),
        center = center,
        radius = radius
    )
    val dropShadow = Brush.radialGradient(
        0.86f to Color.Black.copy(alpha = 0.55f),
        1f to Color.Transparent,
        center = Offset(center.x, center.y + radius * 0.03f),
        radius = radius * 1.06f
    )
    onDrawBehind {
        drawCircle(dropShadow, radius * 1.06f, Offset(center.x, center.y + radius * 0.03f))
        drawCircle(body, radius, center)
    }
}

/** Grooves, sheen and beveled edge, drawn over the cover so it reads as pressed vinyl. */
private fun Modifier.vinylRecordSurface(): Modifier = drawWithCache {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f
    val labelRadius = radius * LABEL_FRACTION
    val hairline = 0.6f * density

    // Light reflected by the grooves: two opposed bright wedges, like a lamp over a real record.
    val sheen = Brush.sweepGradient(
        0.00f to Color.Transparent,
        0.07f to Color.White.copy(alpha = 0.11f),
        0.13f to Color.White.copy(alpha = 0.03f),
        0.20f to Color.Transparent,
        0.50f to Color.Transparent,
        0.57f to Color.White.copy(alpha = 0.08f),
        0.63f to Color.White.copy(alpha = 0.02f),
        0.70f to Color.Transparent,
        1.00f to Color.Transparent,
        center = center
    )
    // Soft broad highlight from the upper left gives the disc volume.
    val gloss = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.07f),
        1f to Color.Transparent,
        center = Offset(center.x - radius * 0.45f, center.y - radius * 0.5f),
        radius = radius * 0.9f
    )
    // Beveled edge: lit on top-left, in shadow on the bottom-right.
    val rim = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.32f),
        0.5f to Color.White.copy(alpha = 0.06f),
        1f to Color.Black.copy(alpha = 0.6f),
        start = Offset(center.x - radius, center.y - radius),
        end = Offset(center.x + radius, center.y + radius)
    )
    // Groove radii: fine grooves, with a few wider glossy "gaps" between tracks.
    val grooveStart = labelRadius + radius * 0.07f
    val grooveEnd = radius * 0.955f
    val trackGaps = listOf(0.30f, 0.52f, 0.71f, 0.86f).map { grooveStart + (grooveEnd - grooveStart) * it }
    val grooveStep = 1.6f * density
    val grooves = buildList {
        var r = grooveStart
        var i = 0
        while (r < grooveEnd) {
            if (trackGaps.none { kotlin.math.abs(it - r) < 2.4f * density }) add(r to i)
            r += grooveStep
            i++
        }
    }

    onDrawBehind {
        // Grooves: alternating faint light and dark rings read as the spiral's texture.
        grooves.forEach { (r, i) ->
            drawCircle(
                color = if (i % 2 == 0) Color.White.copy(alpha = if (i % 7 == 0) 0.06f else 0.035f)
                else Color.Black.copy(alpha = 0.45f),
                radius = r,
                center = center,
                style = Stroke(width = hairline)
            )
        }
        // Track gaps: smooth bands that catch a little more light.
        trackGaps.forEach { r ->
            drawCircle(Color.White.copy(alpha = 0.045f), r, center, style = Stroke(width = 2.2f * density))
        }
        // Lead-out: smooth glossy vinyl around the label, with a single run-out groove.
        drawCircle(Color(0xFF111113).copy(alpha = 0.35f), grooveStart, center)
        drawCircle(Color.White.copy(alpha = 0.05f), labelRadius + radius * 0.035f, center, style = Stroke(width = hairline))

        drawCircle(sheen, radius, center)
        drawCircle(gloss, radius, center)

        // Edge: thin bevel plus a darker lip just inside it.
        drawCircle(Color.Black.copy(alpha = 0.7f), radius * 0.975f, center, style = Stroke(width = 1.4f * density))
        drawCircle(rim, radius - 0.75f * density, center, style = Stroke(width = 1.5f * density))
    }
}

private fun Modifier.labelAndSpindle(): Modifier = drawWithCache {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f
    // Printed-paper depth: the label darkens slightly towards its edge.
    val labelShade = Brush.radialGradient(
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.32f),
        center = center,
        radius = radius
    )
    val labelGloss = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.10f),
        0.45f to Color.Transparent,
        start = Offset(center.x - radius, center.y - radius),
        end = Offset(center.x + radius, center.y + radius)
    )
    val spindleRadius = radius * 0.085f
    val spindleMetal = Brush.radialGradient(
        0f to Color(0xFFF4F5F6),
        0.45f to Color(0xFFB9BDC1),
        1f to Color(0xFF55595D),
        center = Offset(center.x - spindleRadius * 0.35f, center.y - spindleRadius * 0.35f),
        radius = spindleRadius * 1.3f
    )
    onDrawBehind {
        drawCircle(labelShade, radius, center)
        drawCircle(labelGloss, radius, center)
        // Pressed ring where the label meets the vinyl, with a fine gold line (VinylPlayer accent).
        drawCircle(Color.Black.copy(alpha = 0.55f), radius - 1.2f * density, center, style = Stroke(width = 2.4f * density))
        drawCircle(VinylPalette.Gold.copy(alpha = 0.55f), radius - 3.2f * density, center, style = Stroke(width = 0.8f * density))
        // Small concentric rings printed around the center hole.
        drawCircle(Color.Black.copy(alpha = 0.25f), radius * 0.24f, center, style = Stroke(width = 0.8f * density))
        drawCircle(Color.Black.copy(alpha = 0.18f), radius * 0.17f, center, style = Stroke(width = 0.6f * density))
        // Spindle: shadow, chrome cap, center highlight.
        drawCircle(Color.Black.copy(alpha = 0.55f), spindleRadius * 1.45f, Offset(center.x + spindleRadius * 0.25f, center.y + spindleRadius * 0.35f))
        drawCircle(spindleMetal, spindleRadius, center)
        drawCircle(Color.Black.copy(alpha = 0.35f), spindleRadius, center, style = Stroke(width = 0.8f * density))
        drawCircle(Color.White.copy(alpha = 0.85f), spindleRadius * 0.22f, Offset(center.x - spindleRadius * 0.35f, center.y - spindleRadius * 0.35f))
    }
}

// ---------------------------------------------------------------------------------------------
// Tonearm
// ---------------------------------------------------------------------------------------------

/**
 * Same geometry as before: pivot at (82 %, 13 %), resting at (82 %, 52 %), stylus at (68 %, 76 %)
 * when on the record, interpolated by [progress]. Only the look changed.
 */
private fun Modifier.tonearm(progress: () -> Float): Modifier = drawWithCache {
    val min = size.minDimension
    val pivot = Offset(size.width * 0.82f, size.height * 0.13f)
    val parked = Offset(size.width * 0.82f, size.height * 0.52f)
    val recordPoint = Offset(size.width * 0.68f, size.height * 0.76f)

    val tubeWidth = min * 0.022f
    val baseRadius = min * 0.075f
    val headLength = min * 0.13f
    val headWidth = min * 0.058f
    val weightLength = min * 0.075f
    val weightWidth = min * 0.06f
    val weightGap = min * 0.045f
    val shadowOffset = Offset(min * 0.014f, min * 0.022f)

    // Brushed metal across the width of a part lying along +x (drawn in the arm's own frame).
    fun metal(width: Float, dark: Color, mid: Color, light: Color) = Brush.verticalGradient(
        0f to dark,
        0.28f to light,
        0.5f to mid,
        1f to dark,
        startY = -width / 2f,
        endY = width / 2f
    )
    val tubeMetal = metal(tubeWidth, Color(0xFF5E6266), Color(0xFFB7BCC0), Color(0xFFF2F4F5))
    val weightMetal = metal(weightWidth, Color(0xFF26282A), Color(0xFF6B6F73), Color(0xFFC9CDD1))
    val headMetal = metal(headWidth, Color(0xFF0E0E0F), Color(0xFF26272A), Color(0xFF5A5D61))
    val baseMetal = Brush.radialGradient(
        0f to Color(0xFF3A3C3F),
        0.7f to Color(0xFF1B1C1E),
        1f to Color(0xFF0C0C0D),
        center = Offset(pivot.x - baseRadius * 0.3f, pivot.y - baseRadius * 0.3f),
        radius = baseRadius * 1.4f
    )
    val capMetal = Brush.radialGradient(
        0f to Color(0xFFF6F7F8),
        0.5f to Color(0xFFAEB3B7),
        1f to Color(0xFF4E5256),
        center = Offset(pivot.x - baseRadius * 0.15f, pivot.y - baseRadius * 0.15f),
        radius = baseRadius * 0.55f
    )

    onDrawWithContent {
        val p = progress()
        val stylus = Offset(
            x = parked.x + (recordPoint.x - parked.x) * p,
            y = parked.y + (recordPoint.y - parked.y) * p
        )
        val length = hypot(stylus.x - pivot.x, stylus.y - pivot.y)
        val angle = Math.toDegrees(atan2(stylus.y - pivot.y, stylus.x - pivot.x).toDouble()).toFloat()

        // Shadow cast onto the record (same shapes, offset and translucent).
        translate(shadowOffset.x, shadowOffset.y) {
            rotate(angle, pivot) {
                armShapes(pivot, length, tubeWidth, headLength, headWidth, weightLength, weightWidth, weightGap) { _ ->
                    SolidShade
                }
            }
        }

        // Base plate with a gold trim ring.
        drawCircle(Color.Black.copy(alpha = 0.5f), baseRadius * 1.08f, pivot + shadowOffset * 0.6f)
        drawCircle(baseMetal, baseRadius, pivot)
        drawCircle(VinylPalette.Gold.copy(alpha = 0.6f), baseRadius * 0.92f, pivot, style = Stroke(width = 0.9f * density))
        drawCircle(Color.White.copy(alpha = 0.10f), baseRadius, pivot, style = Stroke(width = 0.8f * density))

        rotate(angle, pivot) {
            armShapes(pivot, length, tubeWidth, headLength, headWidth, weightLength, weightWidth, weightGap) { part ->
                when (part) {
                    ArmPart.Tube -> tubeMetal
                    ArmPart.Weight -> weightMetal
                    ArmPart.Head -> headMetal
                }
            }
            // Stylus: cantilever with a gold tip, at the exact point it touches the record.
            val tip = Offset(pivot.x + length, pivot.y)
            drawLine(Color(0xFF9DA2A6), Offset(tip.x - headLength * 0.12f, tip.y + headWidth * 0.18f), tip, strokeWidth = min * 0.006f, cap = StrokeCap.Round)
            drawCircle(VinylPalette.Gold, min * 0.006f, tip)
        }

        // Pivot bearing on top of the arm.
        drawCircle(Color.Black.copy(alpha = 0.45f), baseRadius * 0.5f, pivot + Offset(1.2f * density, 1.8f * density))
        drawCircle(capMetal, baseRadius * 0.45f, pivot)
        drawCircle(Color.Black.copy(alpha = 0.4f), baseRadius * 0.45f, pivot, style = Stroke(width = 0.8f * density))
        drawCircle(Color(0xFF1A1B1D), baseRadius * 0.12f, pivot)
    }
}

private val SolidShade = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.32f)))

private enum class ArmPart { Tube, Weight, Head }

/**
 * The arm laid out along +x from [pivot] (callers rotate the canvas to the real angle):
 * counterweight behind the pivot, the tube, then the headshell angled like a real offset head,
 * ending at the stylus point (pivot.x + length).
 */
private fun DrawScope.armShapes(
    pivot: Offset,
    length: Float,
    tubeWidth: Float,
    headLength: Float,
    headWidth: Float,
    weightLength: Float,
    weightWidth: Float,
    weightGap: Float,
    brush: (ArmPart) -> Brush
) {
    // Counterweight: short stub plus a ribbed cylinder behind the pivot.
    translate(pivot.x - weightGap - weightLength, pivot.y) {
        drawRect(brush(ArmPart.Tube), Offset(weightLength * 0.6f, -tubeWidth * 0.4f), Size(weightGap + weightLength * 0.4f, tubeWidth * 0.8f))
        drawRoundRect(brush(ArmPart.Weight), Offset(0f, -weightWidth / 2f), Size(weightLength, weightWidth), CornerRadius(weightWidth * 0.18f))
        for (i in 1..3) {
            val x = weightLength * (0.18f + i * 0.17f)
            drawLine(Color.Black.copy(alpha = 0.28f), Offset(x, -weightWidth / 2f), Offset(x, weightWidth / 2f), strokeWidth = 0.8f * density)
        }
    }
    // Headshell: angled ~20° around the stylus point, with a finger lift. Its back end is where
    // the tube must meet it.
    val headAngle = 20f
    val headBack = headLength * 0.82f
    val radians = Math.toRadians(headAngle.toDouble())
    val jointX = length - headBack * kotlin.math.cos(radians).toFloat()
    val jointY = -headBack * kotlin.math.sin(radians).toFloat()
    // Tube: from the pivot straight to that joint (a few degrees off the stylus line, as on a real arm).
    translate(pivot.x, pivot.y) {
        rotate(Math.toDegrees(atan2(jointY.toDouble(), jointX.toDouble())).toFloat(), Offset.Zero) {
            drawRoundRect(
                brush(ArmPart.Tube),
                Offset(0f, -tubeWidth / 2f),
                Size(hypot(jointX, jointY) + tubeWidth / 2f, tubeWidth),
                CornerRadius(tubeWidth / 2f)
            )
        }
    }
    rotate(headAngle, Offset(pivot.x + length, pivot.y)) {
        translate(pivot.x + length - headLength * 0.82f, pivot.y) {
            drawRoundRect(brush(ArmPart.Head), Offset(0f, -headWidth / 2f), Size(headLength, headWidth), CornerRadius(headWidth * 0.22f))
            drawRoundRect(brush(ArmPart.Tube), Offset(headLength * 0.18f, -headWidth * 0.32f), Size(headLength * 0.5f, headWidth * 0.64f), CornerRadius(headWidth * 0.12f))
            drawLine(brush(ArmPart.Tube), Offset(headLength * 0.25f, -headWidth / 2f), Offset(headLength * 0.05f, -headWidth * 1.15f), strokeWidth = tubeWidth * 0.45f, cap = StrokeCap.Round)
        }
    }
}
