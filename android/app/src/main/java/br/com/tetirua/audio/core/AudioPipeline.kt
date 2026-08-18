package br.com.tetirua.audio.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Estados observáveis pelo aplicativo e pelo orquestrador. */
enum class AudioPipelineState {
    LOW_POWER_LISTENING,
    WAKE_WORD_CONFIRMED,
    WAITING_FOR_SPEECH,
    CAPTURING_SPEECH,
    FINALIZING_STT,
    HANDOFF_TO_ORCHESTRATOR,
    SYNTHESIZING,
    PLAYING,
    COMPLETED,
    CANCELLED,
    ERROR,
}

/**
 * Coordenador pequeno e independente de engine. A lógica de LLM/VLM/GPS não
 * entra aqui; ela será chamada entre `transcribe` e `speak` pelo orquestrador.
 */
class AudioPipeline(
    private val stt: SpeechToTextEngine,
    private val tts: TextToSpeechEngine,
    private val onStateChanged: (AudioPipelineState) -> Unit = {},
) {
    suspend fun transcribeAndSpeak(
        transcriptionRequest: TranscriptionRequest,
        answerText: String,
        synthesisRequestFactory: (AudioIds, String) -> SynthesisRequest,
        onResult: (TranscriptionResult, SynthesisResult) -> Unit,
    ) {
        onStateChanged(AudioPipelineState.FINALIZING_STT)
        val transcription = withContext(Dispatchers.Default) {
            stt.transcribe(transcriptionRequest)
        }
        if (transcription.text.isBlank()) {
            onStateChanged(AudioPipelineState.ERROR)
            return
        }

        onStateChanged(AudioPipelineState.HANDOFF_TO_ORCHESTRATOR)
        onStateChanged(AudioPipelineState.SYNTHESIZING)
        tts.speak(synthesisRequestFactory(transcription.ids, answerText)) { synthesis ->
            onStateChanged(AudioPipelineState.PLAYING)
            onResult(transcription, synthesis)
            onStateChanged(AudioPipelineState.COMPLETED)
        }
    }

    fun cancel() {
        tts.stop()
        onStateChanged(AudioPipelineState.CANCELLED)
    }
}
