# Integração dos engines no Android — Piper + sherpa-onnx

## Princípio

Os contratos em `core/` são estáveis para o orquestrador, mas cada branch de experimento possui sua própria aplicação e runtime. Nesta branch, **sherpa-onnx é uma dependência exclusiva de Piper**. Não há Whisper.cpp, Moonshine, Kokoro ou Android TTS como runtime principal.

| Componente | Implementação desta branch | Artefato/modelo |
|---|---|---|
| TTS | `PiperSherpaOnnxTtsEngine` | `OfflineTts` com `OfflineTtsVitsModelConfig`. |
| Runtime | API Kotlin oficial + JNI sherpa-onnx | Release Android `v1.13.6`. |
| Voz | Piper `pt_BR-faber-medium-int8` | Modelo da release `tts-models`. |
| Saída | WAV salvo em cache e playback via `MediaPlayer` | 16-bit/PCM conforme `GeneratedAudio.save()`. |
| Wake word | Não implementada nesta Activity | Continua contrato para futura integração. |
| VAD | Não implementada nesta Activity | Continua contrato para futura integração. |

## Configuração VITS/Piper

A API oficial `Tts.kt` define `OfflineTtsVitsModelConfig` com `model`, `tokens`, `dataDir` e `lexicon`. O adaptador usa:

```kotlin
val model = OfflineTtsVitsModelConfig(
    model = "vits-piper-pt_BR-faber-medium-int8/pt_BR-faber-medium.onnx",
    tokens = "vits-piper-pt_BR-faber-medium-int8/tokens.txt",
    dataDir = "vits-piper-pt_BR-faber-medium-int8/espeak-ng-data",
    lexicon = "",
)
```

A configuração é carregada via `OfflineTts(context.assets, config)`. Os caminhos existem somente depois que `fetch-piper-ptbr-model.sh` extrai o modelo para o diretório local incluído como assets.

## Bibliotecas nativas

O script `fetch-sherpa-onnx-android.sh` baixa e verifica a release oficial, instalando as bibliotecas por ABI em `android/local-native-libs/jniLibs/`. O módulo Gradle aponta `sourceSets.main.jniLibs` para esse diretório:

```text
android/local-native-libs/jniLibs/arm64-v8a/libonnxruntime.so
android/local-native-libs/jniLibs/arm64-v8a/libsherpa-onnx-jni.so
android/local-native-libs/jniLibs/armeabi-v7a/libonnxruntime.so
```

As bibliotecas são ignoradas pelo Git. Isso mantém o fork leve e permite trocar a versão somente por decisão explícita.

## Troca de alternativa

Para comparar com Kokoro, Android TTS, Moonshine ou Whisper, faça `git switch` para a branch correspondente. Não copie a pasta `local-native-libs` entre branches como se fosse um runtime comum; cada branch deve documentar sua própria versão e artefatos.

## Referências

- [API Kotlin TTS oficial](https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/kotlin-api/Tts.kt)
- [Repositório sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
- [Documentação TTS](https://k2-fsa.github.io/sherpa/onnx/tts/index.html)
- [Release Android v1.13.6](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.6)
- [Release de modelos TTS](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models)
