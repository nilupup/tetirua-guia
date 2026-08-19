# Integração dos engines no Android — Kokoro-82M

## Princípio

Os contratos em `core/` são compartilhados como interface, mas cada branch possui sua aplicação e seu runtime. Nesta branch, **sherpa-onnx é exclusivo do Kokoro**. Não há Piper, Whisper.cpp, Moonshine ou Android TTS como runtime principal.

| Componente | Implementação desta branch | Artefato/modelo |
|---|---|---|
| TTS | `KokoroTtsEngine` | `OfflineTts` com `OfflineTtsKokoroModelConfig`. |
| Runtime | API Kotlin oficial + JNI sherpa-onnx | Release Android `v1.13.6`. |
| Modelo | Kokoro-82M v1.1 int8 | `model.int8.onnx`, `voices.bin`, `tokens.txt`. |
| Voz | `af_maple` | `sid = 0`, inglês. |
| Idiomas | Inglês e chinês | Limitação documentada; não prometer pt-BR. |
| Saída | WAV e playback | `GeneratedAudio.save()` + `MediaPlayer`. |
| Wake word/VAD | Não implementados na Activity | Permanecem contratos para integração posterior. |

## Configuração Kokoro

A configuração segue a documentação oficial do modelo:

```kotlin
val model = OfflineTtsKokoroModelConfig(
    model = "kokoro-int8-multi-lang-v1_1/model.int8.onnx",
    voices = "kokoro-int8-multi-lang-v1_1/voices.bin",
    tokens = "kokoro-int8-multi-lang-v1_1/tokens.txt",
    dataDir = "kokoro-int8-multi-lang-v1_1/espeak-ng-data",
    lexicon = "kokoro-int8-multi-lang-v1_1/lexicon-us-en.txt,kokoro-int8-multi-lang-v1_1/lexicon-zh.txt",
    lang = "en-us",
)
```

O speaker escolhido pela Activity é `af_maple`, `sid = 0`, conforme a tabela oficial do pacote v1.1. O sample rate esperado é 24 kHz segundo a documentação do modelo.

## Bibliotecas nativas

O script `fetch-sherpa-onnx-android.sh` fixa a release `v1.13.6`, verifica o digest e instala bibliotecas por ABI em `android/local-native-libs/jniLibs/`. O Gradle aponta para esse diretório local. Os arquivos `.so` não entram no Git.

## Troca de alternativa

Para comparar com Piper, troque para `feat/tts-piper-sherpa-onnx`; para Android TTS, troque para `feat/tts-native`; para STT, use as branches Moonshine ou Whisper. Não copie os artefatos locais entre branches como se fossem uma dependência comum.

## Referências

- [API Kotlin TTS oficial](https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/kotlin-api/Tts.kt)
- [Kokoro v1.1 no sherpa-onnx](https://k2-fsa.github.io/sherpa/onnx/tts/all/Chinese-English/kokoro-multi-lang-v1_1.html)
- [Repositório sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
- [Release Android v1.13.6](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.6)
- [Release de modelos TTS](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models)
