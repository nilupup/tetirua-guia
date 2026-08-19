# Tetiruã Audio Android

Este diretório contém a base Android/Kotlin para experimentos de áudio do Tetiruã. **Cada branch de modelo representa uma aplicação independente**. A aplicação da branch `feat/stt-whisper-cpp-tflite`, por exemplo, usa apenas whisper.cpp para STT; ela não depende de sherpa-onnx, Moonshine, Kokoro ou Piper.

## Aplicação da branch Whisper

Nesta branch, o fluxo executável é:

```text
microfone Android
    ↓
PCM mono 16 kHz
    ↓
WAV temporário
    ↓
whisper.cpp via JNI
    ↓
TranscriptionResult em pt-BR
```

A Activity grava seis segundos, carrega um modelo Whisper local e exibe a transcrição. Para o experimento de latência, o engine prefere automaticamente o modelo menor, quando ele estiver presente:

```text
android/app/src/main/assets/models/ggml-tiny-q5_1.bin
```

A ordem de seleção automática é `tiny-q5_1`, depois `base-q5_1` e, por fim, `base`:

```text
models/ggml-tiny-q5_1.bin
models/ggml-base-q5_1.bin
models/ggml-base.bin
```

Os arquivos de modelo são ignorados pelo Git e não são enviados ao GitHub. A documentação oficial do whisper.cpp confirma que modelos quantizados podem reduzir memória e, dependendo do hardware, melhorar a eficiência do processamento; por isso esta branch compara explicitamente velocidade e precisão entre as variantes.

## Como executar

Abra a pasta `android/` no Android Studio e sincronize o Gradle. Inicialize o submódulo:

```bash
git submodule update --init --recursive
```

Baixe um ou mais modelos Whisper no formato compatível e coloque-os em `app/src/main/assets/models/`. Para testar a nova otimização, use `ggml-tiny-q5_1.bin`; se ele não existir, o app usará `ggml-base-q5_1.bin` e depois `ggml-base.bin`. Conecte um aparelho Android, habilite a depuração USB, execute o aplicativo e autorize o microfone. Pressione **Gravar 6 segundos e transcrever**.

Esta branch precisa do Android NDK e CMake para compilar o módulo `:whisper`. O módulo usa o código upstream como submódulo em `third_party/whisper.cpp` e compila a biblioteca JNI localmente.

## Estrutura específica

| Caminho | Função |
|---|---|
| `whisper/` | Biblioteca Android independente com binding Kotlin e JNI do whisper.cpp. |
| `app/src/main/java/.../WhisperCppSttEngine.kt` | Adapta Whisper ao contrato de transcrição do Tetiruã. |
| `app/src/main/java/.../WhisperAudioRecorder.kt` | Captura microfone em PCM mono 16 kHz. |
| `app/src/main/java/.../PcmWavReader.kt` | Lê o WAV temporário para o Whisper. |
| `app/src/main/java/.../MainActivity.kt` | Aplicação demonstrativa somente com Whisper STT. |
| `third_party/whisper.cpp` | Submódulo do upstream, sem modelos. |

## Independência entre branches

As outras alternativas não são dependências desta aplicação:

| Branch | Runtime próprio |
|---|---|
| `feat/stt-moonshine` | Moonshine, com seu binding/runtime específico. |
| `feat/stt-whisper-cpp-tflite` | whisper.cpp/TFLite; esta documentação descreve essa branch. |
| `feat/tts-native` | Android TextToSpeech ou AVSpeechSynthesizer no módulo correspondente. |
| `feat/tts-kokoro-82m` | Kokoro-82M com o runtime Android escolhido para essa branch. |
| `feat/tts-piper-sherpa-onnx` | Piper usando sherpa-onnx como runtime desta branch, não como dependência global. |

Trocar de alternativa significa trocar de branch ou de aplicação. Não significa instalar todos os runtimes no mesmo APK.

## Modelos e licenças

Pesos de modelos, bibliotecas nativas e arquivos de áudio não são versionados. Cada branch deve registrar URL, versão, hash, licença, formato e instrução de instalação. A licença do whisper.cpp e de seus componentes deve ser respeitada separadamente antes de distribuir um APK.

## Limitações atuais

O build do Android precisa ser executado em Android Studio com SDK, NDK e CMake instalados. O ambiente de desenvolvimento desta tarefa não possui esses componentes, portanto a validação final do APK deve ser feita em uma máquina Android configurada. O código e a configuração da branch foram preparados para esse teste, mas não devem ser descritos como APK validado até que o build e a execução em aparelho sejam concluídos.
