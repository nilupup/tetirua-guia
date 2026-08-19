package br.com.tetirua.audio.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class WhisperAudioRecorder {
    @SuppressLint("MissingPermission")
    suspend fun recordToWav(
        outputFile: File,
        durationMs: Long = 6000L,
        sampleRate: Int = 16000,
    ) = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minBuffer > 0) { "Não foi possível obter o buffer mínimo do microfone" }

        val bufferSize = minBuffer * 2
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        require(audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord não foi inicializado"
        }

        val samples = ArrayList<Short>(sampleRate * (durationMs / 1000L).toInt())
        val buffer = ShortArray(bufferSize / 2)
        val startedAt = System.currentTimeMillis()

        try {
            audioRecord.startRecording()
            while (System.currentTimeMillis() - startedAt < durationMs) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                require(read > 0) { "Falha na leitura do microfone: $read" }
                for (i in 0 until read) samples += buffer[i]
            }
        } finally {
            runCatching { audioRecord.stop() }
            audioRecord.release()
        }

        writeWav(outputFile, samples.toShortArray(), sampleRate)
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        val dataSize = samples.size * 2
        DataOutputStream(FileOutputStream(file)).use { output ->
            output.writeBytes("RIFF")
            output.writeIntLE(36 + dataSize)
            output.writeBytes("WAVE")
            output.writeBytes("fmt ")
            output.writeIntLE(16)
            output.writeShortLE(1)
            output.writeShortLE(1)
            output.writeIntLE(sampleRate)
            output.writeIntLE(sampleRate * 2)
            output.writeShortLE(2)
            output.writeShortLE(16)
            output.writeBytes("data")
            output.writeIntLE(dataSize)
            samples.forEach { output.writeShortLE(it.toInt()) }
        }
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value shr 8) and 0xFF)
        writeByte((value shr 16) and 0xFF)
        writeByte((value shr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value shr 8) and 0xFF)
    }
}
