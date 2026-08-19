package br.com.tetirua.audio.stt

import ai.moonshine.voice.MicTranscriber
import android.content.Context
import br.com.tetirua.audio.core.AudioIds
import br.com.tetirua.audio.core.SpeechToTextEngine
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TranscriptionResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adaptador real para o Moonshine Voice Android.
 *
 * O artefato oficial gerencia o microfone, o streaming e o cache do modelo.
 * A branch usa somente esse runtime; nenhum código Whisper ou sherpa-onnx é necessário.
 */
class MoonshineSttEngine(
    context: Context,
    private val language: String = "en",
) : SpeechToTextEngine {
    private val appContext = context.applicationContext
    private val loaded = AtomicBoolean(false)
    private var transcriber: MicTranscriber? = null
    private var activeIds: AudioIds? = null
    private var startedAtMs: Long = 0L
    private var finishedText = StringBuilder()
    private var partialText = ""
    private var pendingResult: CompletableDeferred<TranscriptionResult>? = null
    private var partialListener: (String) -> Unit = {}
    private var statusListener: (String) -> Unit = {}

    private val normalizedLanguage = language.substringBefore('-').lowercase()

    /** Carrega o modelo no cache gerenciado pelo Moonshine; é uma operação bloqueante. */
    fun load(
        onProgress: (fraction: Float, file: String) -> Unit = { _, _ -> },
    ) {
        if (loaded.get()) return
        require(normalizedLanguage in SUPPORTED_LANGUAGES) {
            "Moonshine não possui modelo publicado para o idioma $language nesta versão"
        }

        val mic = MicTranscriber(appContext)
            .language(normalizedLanguage)
            .onText { text ->
                partialText = text ?: ""
                partialListener(currentText())
            }
            .onLine { line ->
                val text = line.text ?: ""
                if (text.isNotBlank()) {
                    finishedText.append(text.trim()).append('\n')
                }
                partialText = ""
                partialListener(currentText())
            }
            .onError { error ->
                statusListener("Erro Moonshine: ${error.message ?: error.javaClass.simpleName}")
                pendingResult?.completeExceptionally(error)
            }
            .onProgress { fraction, file -> onProgress(fraction, file) }

        transcriber = mic
        mic.load()
        loaded.set(true)
    }

    /** Inicia uma sessão de escuta. Deve ser chamado fora da thread de UI. */
    fun startListening(
        ids: AudioIds = AudioIds(),
        onPartial: (String) -> Unit = {},
        onStatus: (String) -> Unit = {},
    ) {
        check(loaded.get()) { "O modelo Moonshine ainda não foi carregado" }
        check(activeIds == null) { "Já existe uma sessão Moonshine ativa" }
        activeIds = ids
        startedAtMs = System.currentTimeMillis()
        finishedText = StringBuilder()
        partialText = ""
        partialListener = onPartial
        statusListener = onStatus
        transcriber?.start()
    }

    /** Encerra a sessão e devolve a última transcrição acumulada. */
    fun stopListening(): TranscriptionResult {
        val ids = activeIds ?: AudioIds()
        transcriber?.stop()
        val result = TranscriptionResult(
            ids = ids,
            text = currentText().trim(),
            language = normalizedLanguage,
            isFinal = true,
            confidence = null,
            audioDurationMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L),
            processingTimeMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L),
            engine = ENGINE_ID,
            model = MODEL_ID,
            warnings = if (normalizedLanguage == "en") {
                listOf("A versão oficial consultada não publica modelo pt-BR; esta demonstração usa inglês.")
            } else {
                emptyList()
            },
        )
        pendingResult?.complete(result)
        pendingResult = null
        activeIds = null
        partialListener = {}
        statusListener = {}
        return result
    }

    /**
     * Implementa o contrato genérico para o orquestrador: inicia uma sessão e aguarda stopListening().
     * A Activity de demonstração usa startListening/stopListening para controlar os botões.
     */
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        require(request.languageHint.substringBefore('-').lowercase() == normalizedLanguage) {
            "Idioma da requisição (${request.languageHint}) diferente do idioma configurado ($language)"
        }
        val deferred = CompletableDeferred<TranscriptionResult>()
        withContext(Dispatchers.Main) {
            pendingResult = deferred
            startListening(request.ids)
        }
        return deferred.await()
    }

    fun close() {
        if (activeIds != null) {
            runCatching { transcriber?.stop() }
        }
        transcriber?.close()
        transcriber = null
        activeIds = null
        pendingResult?.cancel()
        pendingResult = null
        loaded.set(false)
    }

    private fun currentText(): String = buildString {
        append(finishedText)
        append(partialText)
    }

    companion object {
        const val ENGINE_ID = "moonshine-voice-android"
        const val MODEL_ID = "Moonshine Voice official Android model"

        // Lista publicada na documentação de modelos consultada em 2026-08-18.
        private val SUPPORTED_LANGUAGES = setOf(
            "ar",
            "en",
            "es",
            "ja",
            "ko",
            "uk",
            "vi",
            "zh",
        )
    }
}
