package br.com.tetirua.audio.core

/** Resultado neutro produzido por um runtime nativo de STT. */
data class NativeTranscription(
    val text: String,
    val language: String? = null,
    val confidence: Float? = null,
    val processingTimeMs: Long? = null,
)

/** Resultado neutro produzido por um runtime nativo de TTS. */
data class NativeAudio(
    val audioPath: String?,
    val sampleRateHz: Int,
    val durationMs: Long?,
    val firstAudioMs: Long? = null,
    val processingTimeMs: Long? = null,
)

interface NativeSttRuntime {
    fun engineName(): String
    fun modelName(): String?
    fun transcribe(audioPath: String, languageHint: String): NativeTranscription
}

interface NativeTtsRuntime {
    fun engineName(): String
    fun modelName(): String?
    fun synthesize(text: String, language: String, voice: String?, speed: Float): NativeAudio
    fun stop()
}

class PortBackedSttEngine(
    private val runtime: NativeSttRuntime,
) : SpeechToTextEngine {
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val startedAt = System.currentTimeMillis()
        val nativeResult = runtime.transcribe(request.audioPath, request.languageHint)
        return TranscriptionResult(
            ids = request.ids,
            text = nativeResult.text,
            language = nativeResult.language ?: request.languageHint,
            isFinal = true,
            confidence = nativeResult.confidence,
            audioDurationMs = null,
            processingTimeMs = nativeResult.processingTimeMs
                ?: (System.currentTimeMillis() - startedAt),
            engine = runtime.engineName(),
            model = runtime.modelName(),
        )
    }
}

class PortBackedTtsEngine(
    private val runtime: NativeTtsRuntime,
) : TextToSpeechEngine {
    override fun speak(request: SynthesisRequest, onComplete: (SynthesisResult) -> Unit) {
        val startedAt = System.currentTimeMillis()
        val audio = runtime.synthesize(
            text = request.text,
            language = request.language,
            voice = request.voice,
            speed = request.speed,
        )
        onComplete(
            SynthesisResult(
                ids = request.ids,
                audioPath = audio.audioPath,
                durationMs = audio.durationMs,
                firstAudioMs = audio.firstAudioMs,
                processingTimeMs = audio.processingTimeMs
                    ?: (System.currentTimeMillis() - startedAt),
                engine = runtime.engineName(),
                model = runtime.modelName(),
                voice = request.voice,
            ),
        )
    }

    override fun stop() = runtime.stop()
}
