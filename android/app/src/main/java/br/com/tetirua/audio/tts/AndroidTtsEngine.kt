package br.com.tetirua.audio.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import br.com.tetirua.audio.core.SynthesisRequest
import br.com.tetirua.audio.core.SynthesisResult
import br.com.tetirua.audio.core.TextToSpeechEngine
import java.io.File
import java.util.Locale

/**
 * Adaptador do TTS nativo do Android.
 *
 * Ele cobre o primeiro caminho funcional de reprodução no celular. Os motores
 * neurais continuam usando a mesma interface TextToSpeechEngine.
 */
class AndroidTtsEngine(
    context: Context,
    private val onReadyChanged: (Boolean) -> Unit = {},
) : TextToSpeechEngine, TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    @Volatile
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.language = Locale.forLanguageTag("pt-BR")
        }
        onReadyChanged(ready)
    }

    fun isReady(): Boolean = ready

    override fun speak(request: SynthesisRequest, onComplete: (SynthesisResult) -> Unit) {
        if (!ready) {
            onComplete(
                SynthesisResult(
                    ids = request.ids,
                    audioPath = null,
                    durationMs = null,
                    firstAudioMs = null,
                    processingTimeMs = null,
                    engine = "android-tts",
                    model = null,
                    voice = request.voice,
                    warnings = listOf("Android TextToSpeech ainda não foi inicializado"),
                ),
            )
            return
        }

        val language = Locale.forLanguageTag(request.language)
        val languageStatus = textToSpeech.setLanguage(language)
        if (languageStatus == TextToSpeech.LANG_MISSING_DATA ||
            languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            onComplete(
                SynthesisResult(
                    ids = request.ids,
                    audioPath = null,
                    durationMs = null,
                    firstAudioMs = null,
                    processingTimeMs = null,
                    engine = "android-tts",
                    model = null,
                    voice = request.voice,
                    warnings = listOf("Idioma ${request.language} não está disponível no mecanismo Android"),
                ),
            )
            return
        }

        request.voice?.let { requestedVoice ->
            textToSpeech.voices.firstOrNull { voice -> voice.name == requestedVoice }?.let { voice ->
                textToSpeech.voice = voice
            }
        }
        textToSpeech.setSpeechRate(request.speed.coerceIn(0.5f, 2.0f))
        val utteranceId = request.ids.requestId
        val startedAt = System.currentTimeMillis()

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit

            override fun onDone(id: String?) {
                if (id != utteranceId) return
                onComplete(
                    SynthesisResult(
                        ids = request.ids,
                        audioPath = request.outputPath,
                        durationMs = System.currentTimeMillis() - startedAt,
                        firstAudioMs = null,
                        processingTimeMs = System.currentTimeMillis() - startedAt,
                        engine = "android-tts",
                        model = null,
                        voice = request.voice,
                    ),
                )
            }

            override fun onError(id: String?) {
                if (id != utteranceId) return
                onComplete(
                    SynthesisResult(
                        ids = request.ids,
                        audioPath = null,
                        durationMs = null,
                        firstAudioMs = null,
                        processingTimeMs = System.currentTimeMillis() - startedAt,
                        engine = "android-tts",
                        model = null,
                        voice = request.voice,
                        warnings = listOf("Android TextToSpeech reportou erro durante a síntese"),
                    ),
                )
            }
        })

        if (request.outputMode == br.com.tetirua.audio.core.TtsOutputMode.FILE && request.outputPath != null) {
            val outputFile = File(request.outputPath)
            outputFile.parentFile?.mkdirs()
            textToSpeech.synthesizeToFile(
                request.text,
                Bundle(),
                outputFile,
                utteranceId,
            )
        } else {
            textToSpeech.speak(
                request.text,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                utteranceId,
            )
        }
    }

    override fun stop() {
        textToSpeech.stop()
    }

    fun release() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
