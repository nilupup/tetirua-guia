package br.com.tetirua.audio.stt

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class VoskPcmWav(
    val samples: ShortArray,
    val sampleRate: Int,
)

/** Leitor mínimo para WAV PCM mono 16-bit usado pelo teste Vosk. */
internal object VoskPcmWavReader {
    fun read(file: File): VoskPcmWav {
        val bytes = file.readBytes()
        require(bytes.size >= 44) { "WAV muito pequeno: ${file.absolutePath}" }
        require(bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray())) {
            "WAV inválido: cabeçalho RIFF ausente"
        }
        require(bytes.copyOfRange(8, 12).contentEquals("WAVE".toByteArray())) {
            "WAV inválido: cabeçalho WAVE ausente"
        }

        val view = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val channels = view.getShort(22).toInt()
        val sampleRate = view.getInt(24)
        val bitsPerSample = view.getShort(34).toInt()
        require(channels == 1) { "Vosk exige WAV mono; canais encontrados: $channels" }
        require(bitsPerSample == 16) { "Vosk exige PCM 16-bit; bits encontrados: $bitsPerSample" }

        var offset = 12
        var dataOffset = -1
        var dataSize = -1
        while (offset + 8 <= bytes.size) {
            val chunkId = bytes.copyOfRange(offset, offset + 4)
            val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
            if (chunkId.contentEquals("data".toByteArray())) {
                dataOffset = offset + 8
                dataSize = chunkSize.coerceAtMost(bytes.size - dataOffset)
                break
            }
            offset += 8 + chunkSize + (chunkSize and 1)
        }

        require(dataOffset >= 0 && dataSize > 0) { "WAV inválido: chunk data ausente" }
        val sampleCount = dataSize / 2
        val samples = ShortArray(sampleCount)
        val pcm = ByteBuffer.wrap(bytes, dataOffset, sampleCount * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (index in samples.indices) {
            samples[index] = pcm.short
        }
        return VoskPcmWav(samples = samples, sampleRate = sampleRate)
    }
}
