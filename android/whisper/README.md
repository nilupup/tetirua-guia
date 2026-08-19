# Módulo Whisper.cpp independente

Este módulo compila o `whisper.cpp` e sua ponte JNI como uma biblioteca Android local. Ele não depende de sherpa-onnx, Moonshine, Kokoro ou Piper.

## Desenho reconstruído

A primeira versão validável do APK usa somente a ABI física do Samsung testado, `arm64-v8a`, e empacota uma única biblioteca nativa chamada `libwhisper.so`. O wrapper Kotlin não tenta inferir nomes de bibliotecas otimizações a partir de `/proc/cpuinfo`; isso evita solicitar arquivos como `libwhisper_v8fp16_va.so` que não foram compilados.

O CMake aplica alinhamento de 16 KB ao target Whisper e aos targets GGML compartilhados, requisito relevante para aparelhos Android recentes que usam páginas de memória de 16 KB. Outras ABIs poderão ser reativadas somente depois de validação individual.

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

Abra `android/` no Android Studio e execute o módulo `app`. A Activity pede permissão de microfone, grava áudio mono PCM 16 kHz por seis segundos e entrega o WAV ao `WhisperCppSttEngine`, que carrega o modelo sob demanda e chama `WhisperContext` pelo JNI em background.

O idioma é enviado como `pt` quando a requisição informa `pt-BR`. O resultado é convertido para o `TranscriptionResult` do Tetiruã e exibido na tela. A Activity mostra estados separados para permissão, gravação, carregamento do modelo, transcrição, conclusão e erro.

## Estrutura

| Arquivo | Responsabilidade |
|---|---|
| `LibWhisper.kt` | Binding Kotlin derivado do exemplo Android oficial; carrega somente `libwhisper.so`. |
| `jni.c` | Ponte JNI adaptada para o pacote `com.whispercpp.whisper` e idioma configurável. |
| `CMakeLists.txt` | Compila `src/whisper.cpp`, o submódulo GGML e aplica flags de alinhamento. |
| `WhisperCppSttEngine.kt` | Adaptador para o contrato do Tetiruã, com carregamento sob demanda. |
| `WhisperAudioRecorder.kt` | Captura do microfone em PCM mono 16 kHz e gravação WAV. |
| `PcmWavReader.kt` | Leitura do WAV mono 16-bit. |

## Build

O ambiente de desenvolvimento remoto não possui Android SDK, NDK ou CMake; portanto, o build final deve ser executado no Android Studio. O projeto inclui `android/local.properties.example` para orientar o caminho local do SDK. Não envie `android/local.properties` ao GitHub.

Na reconstrução inicial, escolha `arm64-v8a` no módulo `whisper`, faça **Clean Project** e depois **Assemble Selected Modules**. Se o build falhar, consulte o primeiro erro do **Build Output**, não apenas avisos do painel Problems.
