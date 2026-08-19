# Integração dos engines no Android

## Princípio de independência

O aplicativo Kotlin depende dos contratos em `core/`, mas cada branch de experimento escolhe seu próprio runtime. **Não existe um runtime comum obrigatório.** Para testar outra alternativa, troque de branch; não adicione todos os runtimes ao mesmo APK.

| Engine | Runtime Android da branch | Integração | Estado |
|---|---|---|---|
| Moonshine | `ai.moonshine:moonshine-voice:0.1.3` | `MicTranscriber` oficial, encapsulado em `MoonshineSttEngine`. | Implementado nesta branch. |
| whisper.cpp | C/C++ + JNI/Java próprio | Binding JNI controlado e submódulo upstream. | Implementado em `feat/stt-whisper-cpp-tflite`. |
| Android TTS | `android.speech.tts.TextToSpeech` | Adaptador `AndroidTtsEngine.kt`. | Base funcional; Activity própria na branch TTS nativa. |
| Kokoro-82M | Runtime ONNX/sherpa-onnx somente na branch Kokoro, se escolhido. | Adaptador `KokoroTtsEngine`. | Em implementação futura. |
| Piper + sherpa-onnx | sherpa-onnx somente nesta branch | `OfflineTts` com configuração VITS/Piper. | Em implementação futura. |
| Wake word/KWS | Motor dedicado ou sherpa-onnx somente na branch que o adotar. | Contrato `WakeWordDetector`. | Contrato/documentação. |
| VAD | Motor dedicado ou sherpa-onnx somente na branch que o adotar. | Contrato `VoiceActivityDetector`. | Contrato/documentação. |

## Moonshine Voice Android

A branch `feat/stt-moonshine` usa o artefato Maven oficial `ai.moonshine:moonshine-voice:0.1.3`. A Activity chama `MicTranscriber.load()`, `start()` e `stop()`, exibindo texto parcial e final. O SDK gerencia o microfone e o cache do modelo. A documentação consultada lista modelos para árabe, inglês, espanhol, japonês, coreano, mandarim, ucraniano e vietnamita; não há modelo pt-BR publicado na versão usada, por isso a demonstração usa inglês e o adaptador emite aviso ao produzir o resultado.

## Whisper.cpp Android

A branch `feat/stt-whisper-cpp-tflite` compila whisper.cpp pelo NDK/CMake, usa JNI próprio e carrega um modelo `.bin` dos assets. Ela não depende de Moonshine nem de sherpa-onnx. O modelo e bibliotecas geradas permanecem fora do Git.

## sherpa-onnx nas branches que o adotarem

Nas branches Kokoro e Piper, sherpa-onnx é runtime exclusivo daquela branch. Bibliotecas nativas são organizadas por ABI, por exemplo:

```text
app/src/main/jniLibs/arm64-v8a/libonnxruntime.so
app/src/main/jniLibs/arm64-v8a/libsherpa-onnx-jni.so
```

A versão das bibliotecas deve ser registrada no README da branch e os binários grandes não devem ser enviados ao repositório sem decisão explícita de empacotamento.

### Kokoro via sherpa-onnx

A configuração deve seguir a API e o pacote de modelo escolhidos, com nomes reais conferidos na versão adotada:

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
```

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

O pacote selecionado precisa ser compatível com o runtime e com o idioma desejado. O adaptador não deve presumir que toda voz Piper ou Kokoro suporta pt-BR.

## Modelos e bibliotecas

Pesos de modelos, arquivos ONNX, modelos Whisper/Moonshine, vozes e bibliotecas nativas `.so` não devem ser enviados ao Git por padrão. O projeto versiona contratos, configuração, instruções de download, hashes/versões esperadas, testes sem pesos grandes e documentação de licença.
