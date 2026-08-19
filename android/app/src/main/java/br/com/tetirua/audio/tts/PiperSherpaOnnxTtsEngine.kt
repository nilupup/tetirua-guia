package br.com.tetirua.audio.tts

import android.content.Context
import android.media.MediaPlayer
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import br.com.tetirua.audio.core.SynthesisRequest
import br.com.tetirua.audio.core.SynthesisResult
import br.com.tetirua.audio.core.TextToSpeechEngine
import br.com.tetirua.audio.core.TtsOutputMode
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * TTS real desta branch: Piper pt_BR executado pelo runtime sherpa-onnx.
 *
 * Os pesos e as bibliotecas JNI são preparados localmente pelos scripts em android/scripts e
 * permanecem fora do Git. A branch não usa Android TTS, Kokoro ou Whisper.
 */
class PiperSherpaOnnxTtsEngine(
    private val context: Context,
    private val modelDirName: String = DEFAULT_MODEL_DIR,
) : TextToSpeechEngine {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var mediaPlayer: MediaPlayer? = null

    /** Inicializa o modelo a partir dos assets locais preparados antes do build. */
    fun load() {
        if (tts != null) return
        val required = listOf(
            "$modelDirName/$MODEL_FILE",
            "$modelDirName/tokens.txt",
            "$modelDirName/espeak-ng-data",
        )
        required.forEach { path ->
            val exists = if (path.endsWith("/espeak-ng-data")) {
                context.assets.list(path)?.isNotEmpty() == true
            } else {
                runCatching { context.assets.open(path).use { } }.isSuccess
            }
            check(exists) {
                "Asset Piper ausente: $path. Execute android/scripts/fetch-piper-ptbr-model.sh antes do build."
            }
        }

        val model = OfflineTtsVitsModelConfig(
            model = "$modelDirName/$MODEL_FILE",
            tokens = "$modelDirName/tokens.txt",
            dataDir = "$modelDirName/espeak-ng-data",
            lexicon = "",
        )
        tts = OfflineTts(
            context.assets,
            OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = model,
                    numThreads = 2,
                    provider = "cpu",
                ),
            ),
        )
    }

    override fun speak(request: SynthesisRequest, onComplete: (SynthesisResult) -> Unit) {
        worker.execute {
            val startedAt = System.currentTimeMillis()
            runCatching {
                load()
                val output = request.outputPath?.let(::File)
                    ?: File(context.cacheDir, "tetirua-${request.ids.requestId}.wav")
                output.parentFile?.mkdirs()
                val generated = requireNotNull(tts).generate(
                    request.text,
                    sid = 0,
                    speed = request.speed,
                )
                check(generated.save(output.absolutePath)) {
                    "sherpa-onnx não conseguiu salvar o WAV em ${output.absolutePath}"
                }

                if (request.outputMode == TtsOutputMode.PLAYBACK) {
                    play(output)
                }

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
        const val ENGINE_ID = "piper-sherpa-onnx-android"
        const val MODEL_ID = "vits-piper-pt_BR-faber-medium-int8"
        const val VOICE_ID = "faber"
        const val DEFAULT_MODEL_DIR = "vits-piper-pt_BR-faber-medium-int8"
        private const val MODEL_FILE = "pt_BR-faber-medium.onnx"
    }
}
