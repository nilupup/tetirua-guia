# Tetiruã Audio Android — Vosk/Kaldi

Esta branch (`feat/stt-vosk-kaldi`) é uma aplicação Android/Kotlin independente para testar **Vosk/Kaldi** como STT offline em português brasileiro. Ela não usa Whisper.cpp, Moonshine, sherpa-onnx, Piper ou Kokoro como dependências de runtime.

## Fluxo desta branch

```text
microfone Android
    ↓
PCM mono 16 kHz / WAV temporário
    ↓
Vosk Recognizer via AAR Android
    ↓
TranscriptionResult em pt-BR
```

A Activity atual grava seis segundos apenas para manter um baseline comparável com as outras branches. O fluxo final do Tetiruã substituirá essa duração fixa por VAD e wake word.

## Modelo e runtime

O modelo usado é `vosk-model-small-pt-0.3`, listado no catálogo oficial do Vosk para português/português brasileiro. Ele é pequeno o suficiente para um primeiro teste móvel e fica fora do Git.

| Item | Valor |
|---|---|
| Modelo | `vosk-model-small-pt-0.3` |
| Idioma | pt-BR/português |
| Runtime | `com.alphacephei:vosk-android:0.3.75@aar` |
| Binding nativo | `net.java.dev.jna:jna:5.18.1@aar` |
| Instalação local | `android/scripts/fetch-vosk-ptbr-model.sh` |
| Diretório de assets | `android/app/src/main/assets/vosk-model-small-pt-0.3/` |
| Política | Modelo, ZIP, bibliotecas nativas e APK não entram no Git |

Fontes: [Vosk](https://alphacephei.com/vosk/), [Vosk Android](https://alphacephei.com/vosk/android), [catálogo de modelos](https://alphacephei.com/vosk/models) e [API oficial](https://github.com/alphacep/vosk-api).

## Como configurar

Abra a pasta `android/` no Android Studio e inicialize a branch:

```bash
git checkout feat/stt-vosk-kaldi
git pull --ff-only origin feat/stt-vosk-kaldi
```

No Git Bash, a partir da raiz do repositório, instale o modelo localmente:

```bash
bash android/scripts/fetch-vosk-ptbr-model.sh
```

No Windows, também é possível baixar o ZIP pelo endereço oficial e descompactá-lo em:

```text
android/app/src/main/assets/vosk-model-small-pt-0.3/
```

O diretório deve conter pelo menos `final.mdl`, `HCLr.fst`, `Gr.fst`, `mfcc.conf`, `phones.txt` e a pasta `ivector/`. O aplicativo copia os assets para `filesDir` na primeira execução porque o construtor Java do Vosk recebe um caminho local de diretório.

Depois de instalar o modelo, sincronize o Gradle, escolha **Build > Clean Project**, depois **Build > Assemble Project** e clique em **Run**. Autorize o microfone no Samsung SM-S921B.

## Teste

Pressione **Gravar 6 segundos e transcrever** e diga:

```text
Olá, eu sou Fernando.
```

Registre o tempo mostrado em `Estado: concluído em ... ms` e o texto exibido. Repita com:

```text
Tetiruã, explique este monumento.
```

A comparação deve usar o mesmo áudio/frase, aparelho e condições das branches Moonshine e Whisper. O tempo desta Activity inclui leitura do WAV, instalação/carregamento inicial do modelo e inferência; a segunda execução, sem usar **Liberar modelo**, mede melhor o custo de inferência aquecido.

## Limitações atuais

A captura ainda é fixa em seis segundos. VAD, wake word “Tetiruã”, streaming incremental e resultados parciais do `getPartialResult()` ainda serão integrados depois. O modelo pequeno tem como principal risco a precisão em fala livre, nomes próprios e perguntas longas; por isso esta branch deve ser avaliada por latência e qualidade, não apenas por conseguir transcrever uma frase curta.

## Independência entre branches

Esta branch foi criada a partir do checkpoint comum `checkpoint/mensagem-1` (`bb20cc5`) e permanece separada das cinco alternativas anteriores. A branch Conformer/RNN-T será desenvolvida em `feat/stt-conformer-rnnt-sherpa-onnx`. Nenhuma dessas branches deve ser mesclada na `main` sem aprovação do Henrique.
