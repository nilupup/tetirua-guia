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
 *
 * A variante quantizada é preferida quando estiver presente nos assets. O
 * fallback mantém o teste atual funcional enquanto o novo modelo ainda não
 * foi baixado para a máquina de desenvolvimento.
 */
class WhisperCppSttEngine(
    private val context: Context,
    requestedModelAssetPath: String? = null,
) : SpeechToTextEngine {
    private val loadLock = Any()
    private val modelAssetPath: String =
        requestedModelAssetPath ?: resolveDefaultModelAsset(context)
    private var whisperContext: WhisperContext? = null

    /** Nome do arquivo efetivamente selecionado, útil para diagnóstico na UI. */
    val selectedModelAssetPath: String
        get() = modelAssetPath

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
        check(assetExists(context, modelAssetPath)) {
            "Modelo Whisper ausente: $modelAssetPath. " +
                "Coloque um dos modelos em app/src/main/assets/models/."
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
        const val QUANTIZED_MODEL_ASSET = "models/ggml-base-q5_1.bin"
        const val DEFAULT_MODEL_ASSET = "models/ggml-base.bin"

        /**
         * Prefere o modelo Q5_1 por ser menor, mas preserva o modelo base
         * atual para que a branch continue executável durante a migração.
         */
        private val MODEL_CANDIDATES = listOf(
            QUANTIZED_MODEL_ASSET,
            DEFAULT_MODEL_ASSET,
        )

        fun resolveDefaultModelAsset(context: Context): String =
            MODEL_CANDIDATES.firstOrNull { assetExists(context, it) }
                ?: DEFAULT_MODEL_ASSET

        private fun assetExists(context: Context, assetPath: String): Boolean =
            runCatching {
                context.assets.open(assetPath).use { }
                true
            }.getOrDefault(false)
    }
}
