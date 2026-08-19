# Tetiruã Audio Android — branch Kokoro-82M

Esta branch é uma aplicação Android/Kotlin independente para testar **Kokoro-82M v1.1 int8** com o runtime **sherpa-onnx**. Ela não usa Piper, Whisper.cpp, Moonshine ou Android TTS como runtime principal.

## O que a aplicação demonstra

A `MainActivity` recebe texto em inglês, gera áudio offline com `OfflineTtsKokoroModelConfig`, usa o speaker `af_maple` (`sid = 0`), salva um WAV local e reproduz o resultado no alto-falante:

```text
texto do orquestrador → Kokoro-82M → sherpa-onnx OfflineTts → WAV → alto-falante
```

A versão oficial do pacote consultado suporta **inglês e chinês**. Português brasileiro não deve ser presumido nesta branch; a Activity informa essa limitação explicitamente.

## Preparação local

As bibliotecas JNI e o modelo não são versionados. Na raiz do repositório, execute:

```bash
./android/scripts/fetch-sherpa-onnx-android.sh
./android/scripts/fetch-kokoro-model.sh
```

O primeiro script baixa o pacote Android oficial do sherpa-onnx `v1.13.6`, valida SHA-256 e instala as bibliotecas por ABI em `android/local-native-libs/jniLibs/`.

O segundo baixa `kokoro-int8-multi-lang-v1_1.tar.bz2`, valida SHA-256 e instala `model.int8.onnx`, `voices.bin`, `tokens.txt`, os dois lexicons e `espeak-ng-data` em `android/local-models/kokoro/`. Esses diretórios são ignorados pelo Git e entram no APK somente durante o build local.

O phonemizer sherpa-onnx não consome `espeak-ng-data` diretamente dos assets. Na primeira inicialização, o engine copia recursivamente essa árvore para o diretório privado externo do app e passa o caminho absoluto em `OfflineTtsKokoroModelConfig.dataDir`. Uma marca local evita repetir a cópia.

## Execução

Abra `android/` no Android Studio. Se necessário, copie `android/local.properties.example` para `android/local.properties` e ajuste `sdk.dir`. Sincronize o Gradle e execute o módulo `app` em um dispositivo ou emulador com uma ABI preparada.

O ambiente remoto do projeto não possui Android SDK completo, portanto a confirmação final do APK deve ser feita em uma máquina com Android Studio. Se o modelo ou as bibliotecas não forem preparados, a Activity exibirá a mensagem de asset ausente em vez de falhar silenciosamente.

## Estrutura

| Arquivo | Responsabilidade |
|---|---|
| `app/src/main/java/com/k2fsa/sherpa/onnx/Tts.kt` | API Kotlin oficial do sherpa-onnx, preservando a licença upstream. |
| `app/src/main/java/br/com/tetirua/audio/tts/KokoroTtsEngine.kt` | Adaptador TTS, configuração Kokoro, geração e playback. |
| `app/src/main/java/br/com/tetirua/audio/MainActivity.kt` | Demonstração independente com texto e speaker inglês. |
| `scripts/fetch-sherpa-onnx-android.sh` | Download verificado das bibliotecas JNI. |
| `scripts/fetch-kokoro-model.sh` | Download verificado do modelo Kokoro int8. |

## Referências oficiais

A implementação segue a [API Kotlin TTS do sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/kotlin-api/Tts.kt), o [repositório oficial](https://github.com/k2-fsa/sherpa-onnx), a documentação do [Kokoro v1.1](https://k2-fsa.github.io/sherpa/onnx/tts/all/Chinese-English/kokoro-multi-lang-v1_1.html), a [release Android v1.13.6](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.6) e a [release de modelos TTS](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models).

## Independência de branches

O sherpa-onnx é exclusivo desta branch. Para testar Piper, troque para `feat/tts-piper-sherpa-onnx`; para Android TTS, troque para `feat/tts-native`; para STT, use as branches Moonshine ou Whisper. Nenhuma branch deve ser mesclada à `main` sem aprovação do Henrique.
