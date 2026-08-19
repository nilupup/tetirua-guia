package br.com.tetirua.audio.tts

import android.content.Context
import android.media.MediaPlayer
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import br.com.tetirua.audio.core.SynthesisRequest
import br.com.tetirua.audio.core.SynthesisResult
import br.com.tetirua.audio.core.TextToSpeechEngine
import br.com.tetirua.audio.core.TtsOutputMode
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * TTS Kokoro-82M desta branch, usando sherpa-onnx como runtime exclusivo.
 *
 * O pacote v1.1 consultado oferece chinês e inglês; portanto esta demonstração não promete pt-BR.
 */
class KokoroTtsEngine(
    private val context: Context,
    private val modelDirName: String = DEFAULT_MODEL_DIR,
    private val speakerId: Int = 0,
) : TextToSpeechEngine {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var mediaPlayer: MediaPlayer? = null

    fun load() {
        if (tts != null) return
        listOf(
            "$modelDirName/model.int8.onnx",
            "$modelDirName/voices.bin",
            "$modelDirName/tokens.txt",
            "$modelDirName/lexicon-us-en.txt",
            "$modelDirName/lexicon-zh.txt",
        ).forEach { path ->
            check(runCatching { context.assets.open(path).use { } }.isSuccess) {
                "Asset Kokoro ausente: $path. Execute android/scripts/fetch-kokoro-model.sh antes do build."
            }
        }
        check(context.assets.list("$modelDirName/espeak-ng-data")?.isNotEmpty() == true) {
            "Asset Kokoro ausente: $modelDirName/espeak-ng-data"
        }

        val model = OfflineTtsKokoroModelConfig(
            model = "$modelDirName/model.int8.onnx",
            voices = "$modelDirName/voices.bin",
            tokens = "$modelDirName/tokens.txt",
            dataDir = "$modelDirName/espeak-ng-data",
            lexicon = "$modelDirName/lexicon-us-en.txt,$modelDirName/lexicon-zh.txt",
            lang = "en-us",
        )
        tts = OfflineTts(
            context.assets,
            OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = model,
                    numThreads = 2,
                    provider = "cpu",
                ),
            ),
        )
        check(speakerId in 0 until requireNotNull(tts).numSpeakers()) {
            "Speaker Kokoro inválido: $speakerId"
        }
    }

    override fun speak(request: SynthesisRequest, onComplete: (SynthesisResult) -> Unit) {
        worker.execute {
            val startedAt = System.currentTimeMillis()
            runCatching {
                require(request.language.substringBefore('-').lowercase() in SUPPORTED_LANGUAGES) {
                    "Kokoro v1.1 desta branch suporta inglês e chinês; idioma recebido: ${request.language}"
                }
                load()
                val output = request.outputPath?.let(::File)
                    ?: File(context.cacheDir, "tetirua-kokoro-${request.ids.requestId}.wav")
                output.parentFile?.mkdirs()
                val generated = requireNotNull(tts).generate(
                    request.text,
                    sid = speakerId,
                    speed = request.speed,
                )
                check(generated.save(output.absolutePath)) {
                    "sherpa-onnx não conseguiu salvar o WAV em ${output.absolutePath}"
                }
                if (request.outputMode == TtsOutputMode.PLAYBACK) play(output)

                SynthesisResult(
                    ids = request.ids,
                    audioPath = output.absolutePath,
                    durationMs = generated.samples.size.toLong() * 1000L / generated.sampleRate,
                    firstAudioMs = System.currentTimeMillis() - startedAt,
                    processingTimeMs = System.currentTimeMillis() - startedAt,
                    engine = ENGINE_ID,
                    model = MODEL_ID,
                    voice = VOICE_ID,
                )
            }.onSuccess(onComplete)
                .onFailure { error ->
                    onComplete(
                        SynthesisResult(
                            ids = request.ids,
                            audioPath = null,
                            durationMs = null,
                            firstAudioMs = null,
                            processingTimeMs = System.currentTimeMillis() - startedAt,
                            engine = ENGINE_ID,
                            model = MODEL_ID,
                            voice = VOICE_ID,
                            warnings = listOf(error.message ?: error.javaClass.simpleName),
                        ),
                    )
                }
        }
    }

    private fun play(output: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(output.absolutePath)
            setOnCompletionListener { player ->
                player.release()
                if (mediaPlayer === player) mediaPlayer = null
            }
            prepare()
            start()
        }
    }

    override fun stop() {
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        stop()
        tts?.release()
        tts = null
        worker.shutdownNow()
    }

    companion object {
        const val ENGINE_ID = "kokoro-82m-sherpa-onnx-android"
        const val MODEL_ID = "kokoro-int8-multi-lang-v1_1"
        const val VOICE_ID = "af_maple (sid 0)"
        const val DEFAULT_MODEL_DIR = "kokoro-int8-multi-lang-v1_1"
        private val SUPPORTED_LANGUAGES = setOf("en", "zh")
    }
}
