package br.com.tetirua.audio.stt

import android.content.Context
import com.whispercpp.whisper.WhisperContext
import br.com.tetirua.audio.core.SpeechToTextEngine
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TranscriptionResult
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * STT real desta branch: whisper.cpp via JNI, sem sherpa-onnx.
 * O modelo deve estar em `app/src/main/assets/models/` durante a build local.
 */
class WhisperCppSttEngine(
    context: Context,
    private val modelAssetPath: String = "models/ggml-base.bin",
) : SpeechToTextEngine {
    private val whisper = WhisperContext.createContextFromAsset(
        context.assets,
        modelAssetPath,
    )

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val startedAt = System.currentTimeMillis()
        val wav = PcmWavReader.read(File(request.audioPath))
        val text = whisper.transcribeData(
            data = wav.samples,
            language = request.languageHint,
            printTimestamp = false,
        ).trim()

        return TranscriptionResult(
            ids = request.ids,
            text = text,
            language = request.languageHint,
            isFinal = true,
            confidence = null,
            audioDurationMs = (wav.samples.size * 1000L) / wav.sampleRate,
            processingTimeMs = System.currentTimeMillis() - startedAt,
            engine = "whisper.cpp",
            model = modelAssetPath.substringAfterLast('/'),
        )
    }

    fun release() {
        runBlocking { whisper.release() }
    }
}
