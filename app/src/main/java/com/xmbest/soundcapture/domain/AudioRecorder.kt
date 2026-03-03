package com.xmbest.soundcapture.domain

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import com.xmbest.soundcapture.data.model.RecordingConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.abs

class AudioRecorder @Inject constructor() {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isPaused = false

    @SuppressLint("MissingPermission")
    fun startRecording(config: RecordingConfig, outputFile: File): Flow<RecordingData> = flow {
        val bufferSizeInBytes = resolveBufferSize(config)
        val bufferSizeInShorts = (bufferSizeInBytes / 2).coerceAtLeast(1)
        val recorder = createAudioRecord(config, bufferSizeInBytes)
        val buffer = ShortArray(bufferSizeInShorts)

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
        buffer: ShortArray,
        channelSize: Int,
        outputStream: FileOutputStream,
        onData: suspend (RecordingData) -> Unit
    ) {
        val readSize = recorder.read(buffer, 0, buffer.size)
        if (readSize <= 0) {
            delay(5)
            return
        }
        outputStream.write(toPcmBytes(buffer, readSize))
        onData(
            RecordingData(
                channelLevels = channelLevels(
                    data = buffer,
                    length = readSize,
                    channelSize = channelSize
                ),
                bytesWritten = readSize.toLong()
            )
        )
    }

    private fun toPcmBytes(buffer: ShortArray, readSize: Int): ByteArray {
        val byteBuffer = ByteArray(readSize * 2)
        for (i in 0 until readSize) {
            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
            byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
        }
        return byteBuffer
    }

    private fun channelLevels(data: ShortArray, length: Int, channelSize: Int): List<Float> {
        if (length <= 0) return List(channelSize) { 0f }
        val safeChannels = channelSize.coerceAtLeast(1)
        val samplesPerChannel = length / safeChannels
        if (samplesPerChannel <= 0) return List(safeChannels) { 0f }

        val channels = ArrayList<ShortArray>(safeChannels)
        repeat(safeChannels) {
            channels.add(ShortArray(samplesPerChannel))
        }

        var i = 0
        var j = 0
        while (j < samplesPerChannel && i < length) {
            channels.forEach { channel ->
                if (i < length) {
                    channel[j] = data[i]
                    i += 1
                }
            }
            j += 1
        }

        return channels.map { channel ->
            var sum = 0f
            var peak = 0f
            channel.forEach { sample ->
                val amplitude = abs(sample / 32768f)
                sum += amplitude
                if (amplitude > peak) peak = amplitude
            }
            val mean = if (channel.isNotEmpty()) sum / channel.size else 0f
            ((mean * 0.6f) + (peak * 0.4f)).coerceIn(0f, 1f)
        }
    }
}

data class RecordingData(
    val channelLevels: List<Float>,
    val bytesWritten: Long
)
