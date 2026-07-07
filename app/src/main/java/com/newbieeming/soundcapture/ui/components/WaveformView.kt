package com.newbieeming.soundcapture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val LEVEL_MIN = 0f
private const val LEVEL_MAX = 1f
private const val MAX_SAMPLES_PER_CHANNEL = 500

@Composable
fun WaveformView(
    channelLevels: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 1.dp,
    barGap: Dp = 1.dp,
    channelCount: Int = 1,
    isActive: Boolean = true
) {
    val channelSamples = remember { mutableStateListOf<MutableList<Float>>() }
    val wasActive = remember { mutableStateOf(isActive) }
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val spacingPx = with(density) { (barWidth + barGap).toPx() }

    LaunchedEffect(isActive) {
        if (isActive && !wasActive.value) {
            channelSamples.clear()
        }
        wasActive.value = isActive
    }

    LaunchedEffect(channelLevels, isActive, channelCount) {
        if (!isActive || channelLevels.isEmpty()) return@LaunchedEffect
        updateChannelSamples(channelSamples, channelLevels, channelCount)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawWaveformBars(
            channelSamples = channelSamples,
            size = size,
            color = color,
            barWidthPx = barWidthPx,
            spacingPx = spacingPx
        )
    }
}

private fun updateChannelSamples(
    channelSamples: SnapshotStateList<MutableList<Float>>,
    channelLevels: List<Float>,
    channelCount: Int
) {
    val safeChannelCount = channelCount.coerceIn(1, 8)

    // 调整通道数量
    while (channelSamples.size < safeChannelCount) {
        channelSamples.add(mutableStateListOf())
    }
    while (channelSamples.size > safeChannelCount) {
        channelSamples.removeAt(channelSamples.lastIndex)
    }

    // 追加各通道新采样
    for (channelIndex in 0 until safeChannelCount) {
        val level = (channelLevels.getOrNull(channelIndex) ?: 0f)
            .coerceIn(LEVEL_MIN, LEVEL_MAX)
        val samples = channelSamples[channelIndex]
        samples.add(level)
        if (samples.size > MAX_SAMPLES_PER_CHANNEL) {
            samples.removeAt(0)
        }
    }
}

private fun DrawScope.drawWaveformBars(
    channelSamples: List<List<Float>>,
    size: Size,
    color: Color,
    barWidthPx: Float,
    spacingPx: Float
) {
    if (channelSamples.isEmpty() || spacingPx <= 0f) return

    val safeChannelCount = channelSamples.size.coerceIn(1, 8)
    val laneHeight = size.height / safeChannelCount
    val maxHalfHeight = laneHeight / 2f
    val maxBars = (size.width / spacingPx).toInt()

    for (channelIndex in 0 until safeChannelCount) {
        val samples = channelSamples[channelIndex]
        if (samples.isEmpty()) continue
        val centerY = laneHeight * (channelIndex + 0.5f)

        // 从最新数据往左取，最多取 maxBars 个
        val count = minOf(samples.size, maxBars)
        val startIndex = samples.size - count

        for (i in 0 until count) {
            val x = i * spacingPx + barWidthPx / 2f
            val amplitude = samples[startIndex + i].coerceIn(LEVEL_MIN, LEVEL_MAX)
            val lineHeight = maxHalfHeight * amplitude
            drawLine(
                color = color,
                start = Offset(x, centerY - lineHeight),
                end = Offset(x, centerY + lineHeight),
                strokeWidth = barWidthPx
            )
        }
    }
}
