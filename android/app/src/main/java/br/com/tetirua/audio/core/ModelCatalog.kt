package br.com.tetirua.audio.core

enum class AudioRole {
    STT,
    TTS,
    WAKE_WORD,
    VAD,
}

data class ModelOption(
    val id: String,
    val role: AudioRole,
    val runtime: String,
    val platformStatus: String,
    val modelHint: String,
    val sourceUrl: String,
    val artifactPolicy: String,
)

/**
 * Catálogo de alternativas. Os artefatos de modelo não são incluídos no APK
 * nem versionados no Git; cada branch adiciona o adaptador e o procedimento
 * de instalação correspondente.
 */
object ModelCatalog {
    val all: List<ModelOption> = listOf(
        ModelOption(
            id = "moonshine",
            role = AudioRole.STT,
            runtime = "Moonshine runtime / binding Android a validar",
            platformStatus = "avaliação Android",
            modelHint = "tiny/base v2",
            sourceUrl = "https://github.com/moonshine-ai/moonshine",
            artifactPolicy = "baixar modelo separadamente",
        ),
        ModelOption(
            id = "whisper-cpp-tflite",
            role = AudioRole.STT,
            runtime = "whisper.cpp JNI ou runtime TFLite",
            platformStatus = "suporte Android oficial no whisper.cpp; TFLite separado",
            modelHint = "tiny/base multilíngue",
            sourceUrl = "https://github.com/ggml-org/whisper.cpp",
            artifactPolicy = "modelo local fora do Git",
        ),
        ModelOption(
            id = "tts-native",
            role = AudioRole.TTS,
            runtime = "Android TextToSpeech / AVSpeechSynthesizer",
            platformStatus = "Android nativo / iOS nativo",
            modelHint = "voz fornecida pela plataforma",
            sourceUrl = "https://developer.android.com/reference/android/speech/tts/TextToSpeech",
            artifactPolicy = "nenhum peso adicional",
        ),
        ModelOption(
            id = "kokoro-82m",
            role = AudioRole.TTS,
            runtime = "sherpa-onnx OfflineTts Kotlin API",
            platformStatus = "implementado nesta branch Android",
            modelHint = "Kokoro int8 v1.1; af_maple sid 0; inglês/chinês",
            sourceUrl = "https://k2-fsa.github.io/sherpa/onnx/tts/all/Chinese-English/kokoro-multi-lang-v1_1.html",
            artifactPolicy = "modelo e JNI baixados pelos scripts; não versionar",
        ),
        ModelOption(
            id = "piper-sherpa-onnx",
            role = AudioRole.TTS,
            runtime = "sherpa-onnx Kotlin/Java",
            platformStatus = "candidato Android direto",
            modelHint = "modelo Piper compatível",
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx",
            artifactPolicy = "modelo Piper fora do Git",
        ),
        ModelOption(
            id = "wake-word-kws",
            role = AudioRole.WAKE_WORD,
            runtime = "KeywordSpotter via sherpa-onnx ou motor dedicado",
            platformStatus = "contrato comum; motor a selecionar",
            modelHint = "keyword Tetiruã",
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx",
            artifactPolicy = "modelo de keyword separado",
        ),
        ModelOption(
            id = "vad-silero-sherpa",
            role = AudioRole.VAD,
            runtime = "Vad via sherpa-onnx",
            platformStatus = "candidato Android direto",
            modelHint = "Silero VAD ou equivalente compatível",
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx",
            artifactPolicy = "modelo VAD fora do Git",
        ),
    )

    /** Runtime/modelo efetivamente exercitado pela Activity desta branch. */
    val active: ModelOption = all.first { it.id == "kokoro-82m" }
}
