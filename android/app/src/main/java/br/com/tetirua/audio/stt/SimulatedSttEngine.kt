package br.com.tetirua.audio.stt

import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TranscriptionResult
import br.com.tetirua.audio.core.SpeechToTextEngine

/**
 * Implementação de teste. Ela não interpreta áudio; devolve um texto fixo para
 * exercitar o contrato e o pipeline no Android antes do binding real.
 */
class SimulatedSttEngine(
    private val simulatedText: String = "Quem projetou este prédio?",
) : SpeechToTextEngine {
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult =
        TranscriptionResult(
            ids = request.ids,
            text = simulatedText,
            language = request.languageHint,
            isFinal = true,
            confidence = null,
            audioDurationMs = null,
            processingTimeMs = 0,
            engine = "simulated-stt",
            model = "contract-test",
            warnings = listOf("STT simulado: substituir por uma branch de engine real"),
        )
}
