package com.newbieeming.soundcapture.domain

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import com.newbieeming.soundcapture.data.model.RecordingConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.abs

class AudioRecorder @Inject constructor() {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isPaused = false

    @SuppressLint("MissingPermission")
    fun startRecording(config: RecordingConfig, outputFile: File): Flow<RecordingData> = flow {
        val bufferSizeInBytes = resolveBufferSize(config)
        val bytesPerSample = bytesPerSample(config.audioFormat)
        val recorder = createAudioRecord(config, bufferSizeInBytes)
        val buffer = ByteArray(bufferSizeInBytes)

        audioRecord = recorder
        recorder.startRecording()
        isRecording = true
        isPaused = false

        FileOutputStream(outputFile).use { outputStream ->
            while (isRecording) {
                if (isPaused) {
                    delay(20)
                    continue
                }
                readAndEmitFrame(
                    recorder = recorder,
                    buffer = buffer,
                    bytesPerSample = bytesPerSample,
                    audioFormat = config.audioFormat,
                    channelSize = config.waveformChannelCount.coerceIn(1, 8),
                    outputStream = outputStream,
                    onData = { emit(it) }
                )
            }
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun resolveBufferSize(config: RecordingConfig): Int {
        val bufferSizeInBytes = AudioRecord.getMinBufferSize(
            config.sampleRate,
            config.channelConfig,
            config.audioFormat
        )
        if (bufferSizeInBytes <= 0) {
            throw IllegalStateException("Invalid AudioRecord buffer size: $bufferSizeInBytes")
        }
        return bufferSizeInBytes
    }

    private fun bytesPerSample(audioFormat: Int): Int {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(config: RecordingConfig, bufferSizeInBytes: Int): AudioRecord {
        val recorder = AudioRecord(
            config.audioSource,
            config.sampleRate,
            config.channelConfig,
            config.audioFormat,
            bufferSizeInBytes
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord initialization failed")
        }
        return recorder
    }

    private suspend fun readAndEmitFrame(
        recorder: AudioRecord,
        buffer: ByteArray,
        bytesPerSample: Int,
        audioFormat: Int,
        channelSize: Int,
        outputStream: FileOutputStream,
        onData: suspend (RecordingData) -> Unit
    ) {
        val chunkBytes = minOf(buffer.size, 512 * bytesPerSample)
        val readBytes = recorder.read(buffer, 0, chunkBytes)
        if (readBytes <= 0) {
            delay(5)
            return
        }
        outputStream.write(buffer, 0, readBytes)
        onData(
            RecordingData(
                channelLevels = channelLevels(
                    data = buffer,
                    bytesRead = readBytes,
                    bytesPerSample = bytesPerSample,
                    audioFormat = audioFormat,
                    channelSize = channelSize
                ),
                bytesWritten = readBytes.toLong()
            )
        )
    }

    private fun channelLevels(
        data: ByteArray,
        bytesRead: Int,
        bytesPerSample: Int,
        audioFormat: Int,
        channelSize: Int
    ): List<Float> {
        val totalSamples = bytesRead / bytesPerSample
        if (totalSamples <= 0) return List(channelSize) { 0f }

        val safeChannels = channelSize.coerceAtLeast(1)
        val samplesPerChannel = totalSamples / safeChannels
        if (samplesPerChannel <= 0) return List(safeChannels) { 0f }

        val amplitudes = FloatArray(totalSamples)
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            amplitudes[i] = when (audioFormat) {
                AudioFormat.ENCODING_PCM_8BIT -> {
                    (data[i].toFloat() / 128f).coerceIn(-1f, 1f)
                }
                AudioFormat.ENCODING_PCM_16BIT -> {
                    (buffer.getShort(i * bytesPerSample).toFloat() / 32768f).coerceIn(-1f, 1f)
                }
                AudioFormat.ENCODING_PCM_FLOAT -> {
                    buffer.getFloat(i * bytesPerSample).coerceIn(-1f, 1f)
                }
                else -> {
                    (buffer.getShort(i * bytesPerSample).toFloat() / 32768f).coerceIn(-1f, 1f)
                }
            }
        }

        return (0 until safeChannels).map { channelIndex ->
            var sum = 0f
            var peak = 0f
            var count = 0
            var i = channelIndex
            while (i < totalSamples && count < samplesPerChannel) {
                val amplitude = abs(amplitudes[i])
                sum += amplitude
                if (amplitude > peak) peak = amplitude
                count++
                i += safeChannels
            }
            val mean = if (count > 0) sum / count else 0f
            ((mean * 0.6f) + (peak * 0.4f)).coerceIn(0f, 1f)
        }
    }
}

data class RecordingData(
    val channelLevels: List<Float>,
    val bytesWritten: Long
)
