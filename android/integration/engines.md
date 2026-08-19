# Integração do engine Vosk/Kaldi no Android

## Princípio

Esta branch (`feat/stt-vosk-kaldi`) é uma aplicação Android/Kotlin independente. O fluxo ativo implementa apenas `SpeechToTextEngine` com o runtime **Vosk Android**, baseado no ecossistema Kaldi. Whisper, Moonshine, sherpa-onnx, Piper, Kokoro e TTS não são dependências executáveis desta branch.

| Componente | Runtime Android | Implementação nesta branch | Modelo/artefato |
|---|---|---|---|
| Vosk/Kaldi STT | `com.alphacephei:vosk-android:0.3.75@aar` + JNA | `VoskSttEngine` | `vosk-model-small-pt-0.3`, fora do Git |
| Gravação baseline | `AudioRecord` | `VoskAudioRecorder` | PCM mono 16 kHz, WAV temporário |
| Leitura de áudio | Kotlin/JVM | `VoskPcmWavReader` | WAV PCM 16-bit mono |
| Instalação do modelo | Android `AssetManager` | `VoskModelInstaller` | Copia assets para `filesDir` |
| VAD | Contrato comum apenas | Ainda não implementado nesta branch | Futuro adaptador independente |
| Wake word “Tetiruã” | Contrato comum apenas | Ainda não implementado nesta branch | Futuro adaptador independente |

## Dependências

As coordenadas foram conferidas no demo Android oficial do Vosk:

```kotlin
implementation("net.java.dev.jna:jna:5.18.1@aar")
implementation("com.alphacephei:vosk-android:0.3.75@aar")
```

Os repositórios `google()` e `mavenCentral()` estão declarados em `settings.gradle.kts`.

## Fluxo de reconhecimento

O `VoskSttEngine` instala o diretório de modelo a partir dos assets na primeira utilização e cria `org.vosk.Model` a partir do caminho local. Para cada transcrição, ele cria um `org.vosk.Recognizer` com taxa de 16.000 Hz, envia `short[]` PCM mono 16-bit por `acceptWaveForm` e chama `getFinalResult()` para obter o texto final em JSON.

```text
AssetManager
    ↓
filesDir/vosk-model-small-pt-0.3/
    ↓
org.vosk.Model
    ↓
org.vosk.Recognizer(16_000 Hz)
    ↓
acceptWaveForm(short[])
    ↓
getFinalResult()
    ↓
TranscriptionResult
```

A API do Vosk exige que a taxa de amostragem informada ao `Recognizer` corresponda ao áudio. Por isso o gravador usa áudio mono PCM de 16 kHz e o leitor valida o WAV antes da inferência.

## Modelo local

O modelo oficial deve ser instalado com:

```bash
bash android/scripts/fetch-vosk-ptbr-model.sh
```

Os arquivos devem ficar em:

```text
android/app/src/main/assets/vosk-model-small-pt-0.3/
```

O diretório precisa conter, entre outros arquivos, `final.mdl`, `HCLr.fst`, `Gr.fst`, `mfcc.conf`, `phones.txt` e `ivector/`. O ZIP e o diretório descompactado são ignorados pelo `.gitignore` e não podem ser adicionados com `git add`.

## Teste no aparelho

Na Activity, pressione **Gravar 6 segundos e transcrever**, diga uma frase em português e aguarde o estado final. O resultado mostra o engine, o modelo e o tempo de processamento. O indicador informa explicitamente que a operação é local e não depende de internet.

O tempo registrado começa depois da gravação e inclui leitura do WAV, instalação/carregamento do modelo quando necessário e inferência. Para comparar inferência aquecida, repita o teste sem pressionar **Liberar modelo**.

## Trabalho futuro

A gravação fixa de seis segundos existe somente como baseline entre branches. O fluxo de produção deverá adicionar VAD para detectar início/fim de fala e uma implementação independente de wake word para “Tetiruã”. Esses componentes não devem ser introduzidos por meio de um runtime compartilhado com outras branches sem uma decisão explícita da arquitetura.

## Referências

- [Vosk](https://alphacephei.com/vosk/)
- [Vosk Android](https://alphacephei.com/vosk/android)
- [Catálogo oficial de modelos](https://alphacephei.com/vosk/models)
- [Vosk API](https://github.com/alphacep/vosk-api)
- [Demo oficial Vosk Android](https://github.com/alphacep/vosk-android-demo)
