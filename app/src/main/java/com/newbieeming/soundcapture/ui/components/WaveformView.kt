package com.newbieeming.soundcapture.ui.components
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val LEVEL_MIN = 0f
private const val LEVEL_MAX = 1f
private const val SMOOTH_PREVIOUS_WEIGHT = 0.65f
private const val SMOOTH_CURRENT_WEIGHT = 0.35f
private const val MIN_NORMALIZATION_MAX = 0.001f

@Composable
fun WaveformView(
    channelLevels: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    maxBars: Int = 80,
    barWidth: Dp = 4.dp,
    barGap: Dp = 8.dp,
    scrollDurationMs: Int = 90,
    amplitudeScale: Float = 1.7f,
    channelCount: Int = 1,
    minLineHeight: Dp = 2.dp,
    isActive: Boolean = true
) {
    val channelSamples = remember { mutableStateListOf<MutableList<Float>>() }
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val spacing = with(density) { (barWidth + barGap).toPx() }
    val barWidthPx = with(density) { barWidth.toPx() }
    val halfBarWidth = barWidthPx / 2f
    val minLineHeightPx = with(density) { minLineHeight.toPx() }

    LaunchedEffect(isActive) {
        if (!isActive) {
            channelSamples.clear()
            offset.snapTo(0f)
        }
    }

    LaunchedEffect(channelLevels, isActive, maxBars, spacing, scrollDurationMs, channelCount) {
        if (!isActive || channelLevels.isEmpty()) return@LaunchedEffect

        updateChannelSamples(
            channelLevels = channelLevels,
            channelSamples = channelSamples,
            channelCount = channelCount,
            maxBars = maxBars
        )

        offset.snapTo(0f)
        offset.animateTo(
            targetValue = spacing,
            animationSpec = tween(durationMillis = scrollDurationMs)
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        drawWaveformBars(
            channelSamples = channelSamples,
            size = size,
            color = color,
            spacing = spacing,
            offset = offset.value,
            halfBarWidth = halfBarWidth,
            barWidthPx = barWidthPx,
            amplitudeScale = amplitudeScale,
            minLineHeightPx = minLineHeightPx
        )
    }
}

private fun updateChannelSamples(
    channelLevels: List<Float>,
    channelSamples: MutableList<MutableList<Float>>,
    channelCount: Int,
    maxBars: Int
) {
    val safeChannelCount = channelCount.coerceIn(1, 8)
    while (channelSamples.size < safeChannelCount) {
        channelSamples.add(mutableStateListOf<Float>())
    }
    while (channelSamples.size > safeChannelCount) {
        channelSamples.removeAt(channelSamples.lastIndex)
    }

    for (channelIndex in 0 until safeChannelCount) {
        val level = (channelLevels.getOrNull(channelIndex) ?: 0f).coerceIn(LEVEL_MIN, LEVEL_MAX)
        appendSample(samples = channelSamples[channelIndex], level = level, maxBars = maxBars)
    }
}

private fun DrawScope.drawWaveformBars(
    channelSamples: List<List<Float>>,
    size: Size,
    color: Color,
    spacing: Float,
    offset: Float,
    halfBarWidth: Float,
    barWidthPx: Float,
    amplitudeScale: Float,
    minLineHeightPx: Float
) {
    if (channelSamples.isEmpty()) return

    val safeChannelCount = channelSamples.size.coerceIn(1, 8)
    val laneHeight = size.height / safeChannelCount
    val maxLineHeightPerLane = laneHeight / 2f
    val visibleBars = visibleBarCount(width = size.width, spacing = spacing)

    for (channelIndex in 0 until safeChannelCount) {
        val reversed = channelSamples[channelIndex].asReversed()
        if (reversed.isEmpty()) continue
        val windowMax = normalizationMax(reversed = reversed, visibleBars = visibleBars)
        val centerY = laneHeight * (channelIndex + 0.5f)

        for (index in 0 until minOf(reversed.size, visibleBars)) {
            val x = size.width - (index * spacing) - offset
            if (x < -halfBarWidth || x > size.width + halfBarWidth) continue

            val normalizedAmplitude = normalizedAmplitude(
                amplitude = reversed[index],
                windowMax = windowMax
            )
            val lineHeight = calculateLineHeight(
                normalizedAmplitude = normalizedAmplitude,
                amplitudeScale = amplitudeScale,
                maxLineHeightPerLane = maxLineHeightPerLane,
                minLineHeightPx = minLineHeightPx
            )
            drawLine(
                color = color,
                start = Offset(x, centerY - lineHeight),
                end = Offset(x, centerY + lineHeight),
                strokeWidth = barWidthPx
            )
        }
    }
}

private fun normalizationMax(reversed: List<Float>, visibleBars: Int): Float {
    return reversed
        .take(visibleBars)
        .maxOrNull()
        ?.coerceAtLeast(MIN_NORMALIZATION_MAX)
        ?: 1f
}

private fun normalizedAmplitude(amplitude: Float, windowMax: Float): Float {
    return (amplitude.coerceIn(LEVEL_MIN, LEVEL_MAX) / windowMax).coerceIn(LEVEL_MIN, LEVEL_MAX)
}

private fun appendSample(samples: MutableList<Float>, level: Float, maxBars: Int) {
    val last = samples.lastOrNull() ?: 0f
    val smoothed = (last * SMOOTH_PREVIOUS_WEIGHT) + (level * SMOOTH_CURRENT_WEIGHT)
    samples.add(smoothed)
    val overflow = samples.size - maxBars
    if (overflow > 0) {
        repeat(overflow) { samples.removeAt(0) }
    }
}

private fun visibleBarCount(width: Float, spacing: Float): Int {
    if (spacing <= 0f) return 0
    return (width / spacing).roundToInt() + 2
}

private fun calculateLineHeight(
    normalizedAmplitude: Float,
    amplitudeScale: Float,
    maxLineHeightPerLane: Float,
    minLineHeightPx: Float
): Float {
    if (normalizedAmplitude <= 0f || maxLineHeightPerLane <= 0f) return 0f
    val scaledHeight = maxLineHeightPerLane * (1f - exp(-normalizedAmplitude * amplitudeScale))
    val minForLane = min(minLineHeightPx, maxLineHeightPerLane)
    return max(scaledHeight, minForLane * normalizedAmplitude)
}
