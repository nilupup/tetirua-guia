# Módulo Whisper.cpp independente

Este módulo compila o whisper.cpp e sua ponte JNI como uma biblioteca Android local. Ele não depende de sherpa-onnx, Moonshine, Kokoro ou Piper.

## Preparação

Inicialize o submódulo upstream:

```bash
git submodule update --init --recursive
```

Instale no Android Studio o SDK, o NDK `25.2.9519653` e o CMake. Copie um modelo multilíngue compatível para:

```text
android/app/src/main/assets/models/ggml-base.bin
```

Modelos `tiny` e `base` são opções iniciais para aparelhos móveis. O modelo não é versionado no Git.

## Execução

Abra `android/` no Android Studio e execute o módulo `app`. A Activity grava áudio mono PCM 16 kHz e entrega o WAV ao `WhisperCppSttEngine`, que chama `WhisperContext` pelo JNI.

O idioma é enviado como `pt` quando a requisição informa `pt-BR`. O resultado é convertido para o `TranscriptionResult` do Tetiruã e exibido na tela.

## Estrutura

- `LibWhisper.kt`: binding Kotlin derivado do exemplo Android oficial.
- `jni.c`: ponte JNI adaptada para o pacote `com.whispercpp.whisper` e idioma configurável.
- `CMakeLists.txt`: compila `src/whisper.cpp` e o submódulo ggml.
- `WhisperCppSttEngine.kt`: adaptador para o contrato do Tetiruã.
- `WhisperAudioRecorder.kt`: captura do microfone.
- `PcmWavReader.kt`: leitura do WAV mono 16-bit.

## Build

O ambiente de desenvolvimento remoto não possui Android SDK, NDK ou CMake; portanto, o build final deve ser executado no Android Studio. O projeto inclui `android/local.properties.example` para orientar o caminho local do SDK. Não envie `android/local.properties` ao GitHub.
