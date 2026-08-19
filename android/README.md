# Tetiruã Audio Android — branch Piper + sherpa-onnx

Esta branch é uma aplicação Android/Kotlin independente para testar **Piper pt_BR** executado pelo runtime **sherpa-onnx**. Ela não usa Whisper.cpp, Moonshine, Kokoro ou Android TTS como runtime da demonstração.

## O que a aplicação demonstra

A `MainActivity` recebe uma resposta textual, gera áudio WAV localmente com `OfflineTts`, toca o resultado pelo alto-falante e mostra na tela o caminho, o modelo, a duração e o engine usado:

```text
texto do orquestrador → Piper VITS pt_BR → sherpa-onnx OfflineTts → WAV → alto-falante
```

A integração com LLM, VLM, GPS e web search permanece fora desta branch. O orquestrador pode enviar uma `SynthesisRequest` em português brasileiro e consumir o `SynthesisResult`.

## Preparação local

As bibliotecas JNI e os pesos não são versionados. Em uma máquina com `curl`, `sha256sum`, `tar` e `bzip2`, execute na raiz do repositório:

```bash
./android/scripts/fetch-sherpa-onnx-android.sh
./android/scripts/fetch-piper-ptbr-model.sh
```

O primeiro script baixa o pacote oficial `sherpa-onnx-v1.13.6-android.tar.bz2`, valida SHA-256 e instala `libonnxruntime.so`, `libsherpa-onnx-jni.so`, `libsherpa-onnx-c-api.so` e `libsherpa-onnx-cxx-api.so` em `android/local-native-libs/jniLibs/<abi>/`.

O segundo baixa `vits-piper-pt_BR-faber-medium-int8.tar.bz2`, valida SHA-256 e instala o modelo, `tokens.txt` e `espeak-ng-data` em `android/local-models/piper/`. O Gradle inclui esses diretórios locais como `jniLibs` e assets, mas o `.gitignore` impede que sejam enviados ao GitHub.

O `espeak-ng-data` não é consumido diretamente dos assets pelo phonemizer Piper. Na primeira inicialização, o engine copia recursivamente essa árvore para o diretório privado externo do aplicativo e passa o caminho absoluto ao sherpa-onnx. Uma marca local evita repetir a cópia em cada execução.

## Execução

Abra `android/` no Android Studio, copie `android/local.properties.example` para `android/local.properties` e ajuste `sdk.dir` para o Android SDK da máquina. Depois sincronize o Gradle e execute o módulo `app` em um aparelho ou emulador compatível com a ABI instalada.

O build exige Android SDK, Gradle Wrapper e uma instalação local das bibliotecas/modelo. O ambiente remoto deste projeto não possui Android SDK/NDK/CMake completos, então a confirmação final do APK deve ser feita em uma máquina com Android Studio.

## Estrutura

| Arquivo | Responsabilidade |
|---|---|
| `app/src/main/java/com/k2fsa/sherpa/onnx/Tts.kt` | API Kotlin oficial do sherpa-onnx, mantida com o cabeçalho de licença upstream. |
| `app/src/main/java/br/com/tetirua/audio/tts/PiperSherpaOnnxTtsEngine.kt` | Adaptador `TextToSpeechEngine`, configuração VITS e geração/reprodução do WAV. |
| `app/src/main/java/br/com/tetirua/audio/MainActivity.kt` | Demonstração independente de geração e playback. |
| `scripts/fetch-sherpa-onnx-android.sh` | Download verificado das bibliotecas nativas oficiais. |
| `scripts/fetch-piper-ptbr-model.sh` | Download verificado do modelo Piper pt_BR oficial. |

## Referências oficiais

A implementação segue a [API Kotlin TTS do sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/kotlin-api/Tts.kt), o [repositório oficial](https://github.com/k2-fsa/sherpa-onnx), a documentação de [TTS](https://k2-fsa.github.io/sherpa/onnx/tts/index.html), o pacote de [bibliotecas Android v1.13.6](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.6) e o pacote de [modelos TTS](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models).

## Independência de branches

O runtime sherpa-onnx é exclusivo desta branch. Para testar Kokoro, troque para `feat/tts-kokoro-82m`; para testar Android TTS, troque para `feat/tts-native`; para STT, troque para a branch Moonshine ou Whisper correspondente. Nenhuma branch deve ser mesclada à `main` sem aprovação do Henrique.
