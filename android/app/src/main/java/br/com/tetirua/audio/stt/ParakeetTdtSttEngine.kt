package br.com.tetirua.audio.stt

import android.content.Context
import br.com.tetirua.audio.core.SpeechToTextEngine
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TranscriptionResult
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * STT desta branch: Parakeet TDT v3 (FastConformer/TDT) via sherpa-onnx.
 * O modelo e o runtime são exclusivos desta aplicação/branch.
 */
class ParakeetTdtSttEngine(
    private val context: Context,
    private val numThreads: Int = 2,
) : SpeechToTextEngine {
    private val recognizerLock = Any()
    private var recognizer: OfflineRecognizer? = null

    val modelName: String = MODEL_ASSET_DIR
    val threadCount: Int = numThreads

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult =
        withContext(Dispatchers.Default) {
            val startedAt = System.currentTimeMillis()
            val wav = withContext(Dispatchers.IO) {
                ParakeetPcmWavReader.read(File(request.audioPath))
            }
            require(request.languageHint.lowercase().startsWith("pt")) {
                "Esta branch está configurada para testar o modelo multilíngue Parakeet com pt. Idioma recebido: ${request.languageHint}"
            }

            val activeRecognizer = getOrCreateRecognizer()
            var text = ""
            activeRecognizer.createStream().use { stream ->
                stream.acceptWaveform(wav.samples, wav.sampleRate)
                activeRecognizer.decode(stream)
                text = activeRecognizer.getResult(stream).text.trim()
            }

            require(text.isNotBlank()) {
                "Parakeet não retornou texto. Confira o áudio, a memória disponível e os arquivos ONNX."
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

    private fun getOrCreateRecognizer(): OfflineRecognizer = synchronized(recognizerLock) {
        recognizer ?: OfflineRecognizer(
            context.assets,
            OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = ParakeetAudioRecorder.SAMPLE_RATE),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = "$MODEL_ASSET_DIR/encoder.int8.onnx",
                        decoder = "$MODEL_ASSET_DIR/decoder.int8.onnx",
                        joiner = "$MODEL_ASSET_DIR/joiner.int8.onnx",
                    ),
                    tokens = "$MODEL_ASSET_DIR/tokens.txt",
                    numThreads = numThreads,
                    provider = "cpu",
                    modelType = "nemo_transducer",
                ),
            ),
        ).also { recognizer = it }
    }

    fun release() {
        val recognizerToRelease = synchronized(recognizerLock) {
            recognizer.also { recognizer = null }
        }
        recognizerToRelease?.release()
    }

    companion object {
        const val ENGINE_ID = "parakeet-tdt-sherpa-onnx"
        const val MODEL_ASSET_DIR = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8"
    }
}
