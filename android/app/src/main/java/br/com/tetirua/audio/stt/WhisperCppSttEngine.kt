package br.com.tetirua.audio.stt

import android.content.Context
import com.whispercpp.whisper.WhisperContext
import br.com.tetirua.audio.core.SpeechToTextEngine
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * STT real desta branch: whisper.cpp via JNI próprio, sem sherpa-onnx.
 * O primeiro alvo Android validado é arm64-v8a.
 */
class WhisperCppSttEngine(
    private val context: Context,
    private val modelAssetPath: String = DEFAULT_MODEL_ASSET,
) : SpeechToTextEngine {
    private val loadLock = Any()
    private var whisperContext: WhisperContext? = null

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult =
        withContext(Dispatchers.Default) {
            val startedAt = System.currentTimeMillis()
            val wav = withContext(Dispatchers.IO) {
                PcmWavReader.read(File(request.audioPath))
            }
            val engine = getOrCreateContext()
            val text = engine.transcribeData(
                data = wav.samples,
                language = request.languageHint.substringBefore('-'),
                printTimestamp = false,
            ).trim()

            require(text.isNotBlank()) {
                "Whisper não retornou segmentos. Verifique a fala, o microfone e o modelo."
            }

            TranscriptionResult(
                ids = request.ids,
                text = text,
                language = request.languageHint,
                isFinal = true,
                confidence = null,
                audioDurationMs = (wav.samples.size * 1000L) / wav.sampleRate,
                processingTimeMs = System.currentTimeMillis() - startedAt,
                engine = ENGINE_ID,
                model = modelAssetPath.substringAfterLast('/'),
            )
        }

    private fun getOrCreateContext(): WhisperContext = synchronized(loadLock) {
        whisperContext ?: run {
            checkAssetExists()
            WhisperContext.createContextFromAsset(context.assets, modelAssetPath).also {
                whisperContext = it
            }
        }
    }

    private fun checkAssetExists() {
        val exists = runCatching {
            context.assets.open(modelAssetPath).use { }
            true
        }.getOrDefault(false)
        check(exists) {
            "Modelo Whisper ausente: $modelAssetPath. Coloque ggml-base.bin em app/src/main/assets/models/."
        }
    }

    fun release() {
        val contextToRelease = synchronized(loadLock) {
            whisperContext.also { whisperContext = null }
        }
        contextToRelease?.let { runBlocking { it.release() } }
    }

    companion object {
        const val ENGINE_ID = "whisper.cpp-jni-android"
        const val DEFAULT_MODEL_ASSET = "models/ggml-base.bin"
    }
}
