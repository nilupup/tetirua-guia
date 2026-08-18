package br.com.tetirua.audio.core

import java.util.UUID

/** Versão do contrato compartilhado entre áudio e orquestrador. */
const val AUDIO_SCHEMA_VERSION = "1.0"

data class AudioIds(
    val sessionId: String = "sess-${UUID.randomUUID()}",
    val turnId: String = "turn-${UUID.randomUUID()}",
    val requestId: String = "req-${UUID.randomUUID()}",
)

data class WakeWordRequest(
    val keyword: String,
    val threshold: Float? = null,
)

data class WakeWordResult(
    val keyword: String,
    val score: Float?,
    val engine: String,
    val model: String?,
)

data class VadSegment(
    val startMs: Long,
    val endMs: Long? = null,
)

data class TranscriptionRequest(
    val ids: AudioIds,
    val audioPath: String,
    val languageHint: String = "pt-BR",
    val streaming: Boolean = false,
)

data class TranscriptionResult(
    val ids: AudioIds,
    val text: String,
    val language: String?,
    val isFinal: Boolean,
    val confidence: Float?,
    val audioDurationMs: Long?,
    val processingTimeMs: Long?,
    val engine: String,
    val model: String?,
    val warnings: List<String> = emptyList(),
)

enum class TtsOutputMode {
    PLAYBACK,
    FILE,
    STREAM,
}

data class SynthesisRequest(
    val ids: AudioIds,
    val text: String,
    val language: String = "pt-BR",
    val voice: String? = null,
    val speed: Float = 1.0f,
    val outputMode: TtsOutputMode = TtsOutputMode.PLAYBACK,
    val outputPath: String? = null,
)

data class SynthesisResult(
    val ids: AudioIds,
    val audioPath: String?,
    val durationMs: Long?,
    val firstAudioMs: Long?,
    val processingTimeMs: Long?,
    val engine: String,
    val model: String?,
    val voice: String?,
    val warnings: List<String> = emptyList(),
)

interface WakeWordDetector {
    fun start(request: WakeWordRequest, onDetected: (WakeWordResult) -> Unit)
    fun stop()
}

interface VoiceActivityDetector {
    fun start(onSpeechStarted: (VadSegment) -> Unit, onSpeechStopped: (VadSegment) -> Unit)
    fun stop()
}

interface SpeechToTextEngine {
    suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult
}

interface TextToSpeechEngine {
    fun speak(request: SynthesisRequest, onComplete: (SynthesisResult) -> Unit = {})
    fun stop()
}

enum class AudioErrorCode {
    AUDIO_PERMISSION_DENIED,
    AUDIO_FORMAT_UNSUPPORTED,
    MODEL_NOT_INSTALLED,
    UNSUPPORTED_LANGUAGE,
    STT_TIMEOUT,
    EMPTY_TRANSCRIPT,
    TTS_UNAVAILABLE,
    NO_SPEECH_AFTER_WAKE_WORD,
    CANCELLED,
}

data class AudioError(
    val code: AudioErrorCode,
    val message: String,
    val component: String,
    val engine: String? = null,
    val retryable: Boolean = false,
    val fallbackEngine: String? = null,
)
