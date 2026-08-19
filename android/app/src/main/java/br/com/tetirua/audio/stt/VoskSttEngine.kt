package br.com.tetirua.audio.stt

import android.content.Context
import br.com.tetirua.audio.core.SpeechToTextEngine
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * STT desta branch: Vosk/Kaldi Android, totalmente offline.
 * O runtime e o modelo são exclusivos desta aplicação/branch.
 */
class VoskSttEngine(
    private val context: Context,
) : SpeechToTextEngine {
    private val modelLock = Any()
    private var model: Model? = null

    val modelName: String = VoskModelInstaller.MODEL_ASSET_DIR

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult =
        withContext(Dispatchers.Default) {
            val startedAt = System.currentTimeMillis()
            val wav = withContext(Dispatchers.IO) {
                VoskPcmWavReader.read(File(request.audioPath))
            }
            require(request.languageHint.lowercase().startsWith("pt")) {
                "Esta branch está configurada para o modelo Vosk pt-BR. Idioma recebido: ${request.languageHint}"
            }

            val recognizer = Recognizer(getOrCreateModel(), wav.sampleRate.toFloat())
            val text = try {
                recognizer.acceptWaveForm(wav.samples, wav.samples.size)
                JSONObject(recognizer.finalResult).optString("text").trim()
            } finally {
                recognizer.close()
            }

            require(text.isNotBlank()) {
                "Vosk não retornou texto. Verifique a fala, o microfone e o modelo pt-BR."
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
                model = modelName,
            )
        }

    private fun getOrCreateModel(): Model = synchronized(modelLock) {
        model ?: run {
            val modelDir = VoskModelInstaller.installIfNeeded(context)
            Model(modelDir.absolutePath).also { model = it }
        }
    }

    fun release() {
        val modelToRelease = synchronized(modelLock) {
            model.also { model = null }
        }
        modelToRelease?.close()
    }

    companion object {
        const val ENGINE_ID = "vosk-kaldi-android"
    }
}
