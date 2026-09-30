package com.chrismdz.vinylplayer.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.atan2

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

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF343434), Color(0xFF101010), Color(0xFF050505))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = "Carátula del disco",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alpha = 0.96f
                )
            }

            VinylGrooves(modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .aspectRatio(1f)
                    .graphicsLayer { rotationZ = rotation.value }
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.88f), CircleShape)
                    .background(Color(0xFFE4B45B)),
                contentAlignment = Alignment.Center
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171717))
                )
            }
        }

        Tonearm(progress = tonearmProgress.value)
    }
}

@Composable
private fun Tonearm(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val pivot = Offset(size.width * 0.82f, size.height * 0.13f)
        val parked = Offset(size.width * 0.82f, size.height * 0.52f)
        val recordPoint = Offset(size.width * 0.68f, size.height * 0.76f)
        val armEnd = Offset(
            x = parked.x + (recordPoint.x - parked.x) * progress,
            y = parked.y + (recordPoint.y - parked.y) * progress
        )
        val armWidth = size.minDimension * 0.026f
        val cartridgeWidth = size.minDimension * 0.065f
        val cartridgeHeight = size.minDimension * 0.12f
        val armAngle = Math.toDegrees(
            atan2(armEnd.y - pivot.y, armEnd.x - pivot.x).toDouble()
        ).toFloat()

        drawCircle(Color.Black.copy(alpha = 0.45f), size.minDimension * 0.065f, pivot)
        drawCircle(Color(0xFF9EA3A5), size.minDimension * 0.035f, pivot)
        drawLine(
            color = Color.Black.copy(alpha = 0.5f),
            start = pivot,
            end = armEnd,
            strokeWidth = armWidth * 1.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFC7CCCE),
            start = pivot,
            end = armEnd,
            strokeWidth = armWidth,
            cap = StrokeCap.Round
        )

        withTransform({ rotate(armAngle + 90f, armEnd) }) {
            drawRoundRect(
                color = Color(0xFF191919),
                topLeft = Offset(armEnd.x - cartridgeWidth / 2f, armEnd.y - cartridgeHeight / 2f),
                size = Size(cartridgeWidth, cartridgeHeight),
                cornerRadius = CornerRadius(cartridgeWidth / 4f)
            )
        }

        drawCircle(Color.Black, size.minDimension * 0.012f, center = armEnd)
    }
}

@Composable
private fun VinylGrooves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        drawCircle(Color.Black.copy(alpha = 0.25f), maxRadius * 0.99f, center)
        for (index in 1..12) {
            val radius = maxRadius * (0.16f + index * 0.065f)
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.82f),
            radius = maxRadius * 0.98f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )
    }
}
