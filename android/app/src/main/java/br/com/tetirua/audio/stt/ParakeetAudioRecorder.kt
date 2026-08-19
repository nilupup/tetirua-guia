package br.com.tetirua.audio.stt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Gravador baseline; o fluxo de produção usará VAD/streaming posteriormente. */
class ParakeetAudioRecorder(
    private val sampleRate: Int = SAMPLE_RATE,
    private val durationMs: Int = 6_000,
) {
    fun recordToWav(output: File) {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minBuffer > 0) { "Não foi possível obter o buffer mínimo do microfone: $minBuffer" }

        val totalSamples = sampleRate * durationMs / 1_000
        val samples = ShortArray(totalSamples)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, totalSamples * 2),
        )
        var offset = 0
        try {
            recorder.startRecording()
            while (offset < samples.size) {
                val read = recorder.read(samples, offset, samples.size - offset)
                require(read > 0) { "Falha ao ler microfone: código $read" }
                offset += read
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }

        writeWav(output, samples)
    }

    private fun writeWav(output: File, samples: ShortArray) {
        output.parentFile?.mkdirs()
        val pcmBytes = ByteArray(samples.size * 2)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            samples.forEach { putShort(it) }
        }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + pcmBytes.size)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(pcmBytes.size)
        }.array()
        FileOutputStream(output).use { stream ->
            stream.write(header)
            stream.write(pcmBytes)
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
