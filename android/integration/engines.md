# Integração dos engines no Android

## Princípio

O aplicativo Kotlin depende apenas dos contratos em `core/`. Cada engine implementa `SpeechToTextEngine` ou `TextToSpeechEngine` por meio de um runtime que pode ser nativo, JNI, ONNX ou uma API da plataforma.

| Engine | Runtime Android | Integração | Dependência de modelo |
|---|---|---|---|
| Moonshine | sherpa-onnx ou binding próprio | Preferir wrapper do runtime e manter `MoonshineSttEngine` atrás de `NativeSttRuntime`. | Modelos Moonshine v2 e tokens/configuração correspondentes. |
| whisper.cpp | C/C++ + JNI/Java | Usar o binding Android/Java oficial ou uma camada JNI controlada. | Modelo Whisper `.bin`/formato suportado, fora do Git. |
| TFLite | TensorFlow Lite/ONNX conforme conversão | Implementar runtime separado, mantendo o mesmo `NativeSttRuntime`. | Arquivos `.tflite` e metadados, fora do Git. |
| Android TTS | `android.speech.tts.TextToSpeech` | Já existe em `tts/AndroidTtsEngine.kt`. | Nenhum peso obrigatório; depende das vozes instaladas no dispositivo. |
| AVSpeechSynthesizer | API nativa iOS | Não pertence ao módulo Android; permanece no contrato multiplataforma. | Vozes da plataforma iOS. |
| Kokoro-82M | sherpa-onnx `OfflineTts`/ONNX | Usar `OfflineTtsKokoroModelConfig` e encapsular geração de `GeneratedAudio`. | Modelo Kokoro, vozes, tokens e dados auxiliares. |
| Piper+sherpa-onnx | sherpa-onnx `OfflineTts`/VITS | Usar configuração VITS e encapsular o WAV ou stream produzido. | Modelo Piper, `tokens.txt`, vocabulário/lexicon quando necessário. |
| Wake word | sherpa-onnx `KeywordSpotter` | Implementar adapter que converte `KeywordSpotterResult` em `WakeWordResult`. | Modelo KWS e arquivo de keywords. |
| VAD | sherpa-onnx `Vad` | Implementar adapter que alimenta `acceptWaveform` e converte segmentos em eventos. | `silero_vad.onnx` ou outro modelo VAD compatível. |

## Integração com sherpa-onnx

A documentação Kotlin oficial do sherpa-onnx fornece classes para `OfflineRecognizer`, `OfflineTts`, `Vad` e `KeywordSpotter`. A aplicação deve copiar ou depender da API Kotlin e distribuir as bibliotecas nativas por ABI:

```text
app/src/main/jniLibs/arm64-v8a/libonnxruntime.so
app/src/main/jniLibs/arm64-v8a/libsherpa-onnx-jni.so
```

Para emulador, pode ser necessário preparar também `x86_64`. A versão das bibliotecas deve ser registrada no README e mantida igual à versão dos arquivos Kotlin/API utilizados.

### Kokoro via sherpa-onnx

A configuração deve seguir a ideia abaixo, com os nomes reais dos arquivos fornecidos pelo pacote de modelo escolhido:

```kotlin
val config = OfflineTtsConfig(
    model = OfflineTtsModelConfig(
        kokoro = OfflineTtsKokoroModelConfig(
            model = "$modelDir/model.onnx",
            voices = "$modelDir/voices.bin",
            tokens = "$modelDir/tokens.txt",
            dataDir = "$modelDir/espeak-ng-data",
            lexicon = "$modelDir/lexicon-us-en.txt",
        ),
        numThreads = 4,
        provider = "cpu",
    ),
)
val tts = OfflineTts(assetManager, config)
val generated = tts.generate(text, sid = voiceId, speed = speed)
generated.save(outputPath)
```

Os nomes e caminhos devem ser conferidos contra o pacote de modelo escolhido. Não devemos colocar esses arquivos grandes no Git apenas para fazer o exemplo compilar.

### Piper via sherpa-onnx

Para Piper, a configuração normalmente utiliza a família VITS do `OfflineTts`:

```kotlin
val config = OfflineTtsConfig(
    model = OfflineTtsModelConfig(
        vits = OfflineTtsVitsModelConfig(
            model = "$modelDir/model.onnx",
            tokens = "$modelDir/tokens.txt",
            lexicon = "$modelDir/lexicon.txt",
            dataDir = "$modelDir/espeak-ng-data",
        ),
        numThreads = 2,
        provider = "cpu",
    ),
)
```

O pacote Piper selecionado precisa ser compatível com o runtime e com o idioma desejado. O adaptador não deve presumir que toda voz Piper suporta `pt-BR`.

### Moonshine e Whisper via sherpa-onnx

A API oficial do `OfflineRecognizer` possui configurações para Whisper e Moonshine. Para Moonshine v2, o catálogo oficial descreve encoder e decoder mesclado; a estrutura exata dos arquivos deve seguir o pacote do modelo. Para Whisper, o adaptador deve informar idioma e tarefa de transcrição:

```kotlin
val whisperConfig = OfflineWhisperModelConfig(
    encoder = "$modelDir/encoder.onnx",
    decoder = "$modelDir/decoder.onnx",
    language = "pt",
    task = "transcribe",
)
```

O resultado de `OfflineRecognizer` é convertido para `TranscriptionResult`. O orquestrador não recebe objetos do sherpa-onnx diretamente.

## O que ainda precisa ser implementado em cada branch

A base comum já contém contratos, catálogo, pipeline, TTS Android funcional e adapters por porta. Cada branch de engine deve substituir a porta genérica pelo runtime concreto, adicionar os arquivos de configuração necessários, documentar o download do modelo e fornecer pelo menos um teste em aparelho ou emulador.

A ausência de pesos ou bibliotecas nativas nesta primeira publicação é intencional. Isso mantém o repositório leve, evita problemas de licença e permite que cada colaborador escolha a variante de modelo sem duplicar artefatos grandes.
