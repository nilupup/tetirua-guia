package br.com.tetirua.audio.stt

import java.io.File
import java.io.RandomAccessFile

internal data class PcmWav(
    val samples: FloatArray,
    val sampleRate: Int,
)

/** Leitor mínimo e explícito para WAV PCM mono 16-bit usado pelo branch Whisper. */
internal object PcmWavReader {
    fun read(file: File): PcmWav {
        RandomAccessFile(file, "r").use { input ->
            require(readAscii(input, 4) == "RIFF") { "Arquivo não é um WAV RIFF" }
            input.skipBytes(4)
            require(readAscii(input, 4) == "WAVE") { "Arquivo não é WAV" }

            var audioFormat = -1
            var channels = -1
            var sampleRate = -1
            var bitsPerSample = -1
            var dataOffset = -1L
            var dataSize = -1

            while (input.filePointer + 8 <= input.length()) {
                val chunkId = readAscii(input, 4)
                val chunkSize = readLittleEndianInt(input)
                when (chunkId) {
                    "fmt " -> {
                        audioFormat = readLittleEndianShort(input)
                        channels = readLittleEndianShort(input)
                        sampleRate = readLittleEndianInt(input)
                        input.skipBytes(6)
                        bitsPerSample = readLittleEndianShort(input)
                        val remaining = chunkSize - 16
                        if (remaining > 0) input.skipBytes(remaining)
                    }
                    "data" -> {
                        dataOffset = input.filePointer
                        dataSize = chunkSize
                        input.skipBytes(chunkSize)
                    }
                    else -> input.skipBytes(chunkSize)
                }
                if (chunkSize % 2 != 0) input.skipBytes(1)
            }

            require(audioFormat == 1) { "Apenas PCM sem compressão é suportado" }
            require(channels == 1) { "O Whisper Android exige WAV mono" }
            require(sampleRate == 16000) { "A taxa deve ser 16000 Hz; encontrada $sampleRate Hz" }
            require(bitsPerSample == 16) { "Apenas PCM 16-bit é suportado" }
            require(dataOffset >= 0 && dataSize > 0) { "Chunk data não encontrado" }

            input.seek(dataOffset)
            val sampleCount = dataSize / 2
            val samples = FloatArray(sampleCount)
            for (i in samples.indices) {
                val signed = readLittleEndianShort(input).toShort().toInt()
                samples[i] = signed / 32768.0f
            }
            return PcmWav(samples, sampleRate)
        }
    }

    private fun readAscii(input: RandomAccessFile, count: Int): String {
        val bytes = ByteArray(count)
        input.readFully(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    private fun readLittleEndianShort(input: RandomAccessFile): Int {
        val low = input.readUnsignedByte()
        val high = input.readUnsignedByte()
        return low or (high shl 8)
    }

    private fun readLittleEndianInt(input: RandomAccessFile): Int {
        val b0 = input.readUnsignedByte()
        val b1 = input.readUnsignedByte()
        val b2 = input.readUnsignedByte()
        val b3 = input.readUnsignedByte()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
}
