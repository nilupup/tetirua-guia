# Tetiruã Audio Android — Parakeet TDT / sherpa-onnx

Esta branch (`feat/stt-conformer-rnnt-sherpa-onnx`) é uma aplicação Android/Kotlin independente para testar o **Parakeet TDT v3** com o runtime **sherpa-onnx**. Ela não usa Vosk, Whisper.cpp, Moonshine, Piper ou Kokoro como runtime da demonstração.

## O que cada nome significa

```text
Parakeet TDT v3 = modelo concreto da NVIDIA
FastConformer    = arquitetura do encoder do modelo
TDT/transducer   = mecanismo de decodificação incremental
sherpa-onnx      = runtime Android/Kotlin que executa os arquivos ONNX
```

O modelo e o runtime ficam juntos nesta branch porque o modelo precisa de um executor para funcionar, mas são artefatos conceitualmente diferentes. O modelo Parakeet v3 listado pelo sherpa-onnx suporta 25 idiomas europeus, incluindo `pt`; a variante de português deve ser validada no Samsung, pois a documentação da NVIDIA alerta que parte dos dados pode refletir português europeu.

## Fluxo desta branch

```text
microfone Android
    ↓
PCM mono 16 kHz / WAV temporário
    ↓
FloatArray normalizado
    ↓
sherpa-onnx OfflineRecognizer
    ↓
Parakeet TDT v3 INT8: encoder + decoder + joiner
    ↓
TranscriptionResult em pt
```

A Activity grava seis segundos apenas para criar um baseline comum. A integração de produção deverá trocar essa captura fixa por VAD e, posteriormente, por reconhecimento streaming.

## Runtime e modelo

| Item | Valor |
|---|---|
| Modelo | `sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8` |
| Família | FastConformer/TDT/transducer |
| Idiomas | 25 idiomas europeus, incluindo `pt` |
| Tamanho aproximado | 640 MB descompactado |
| Runtime | sherpa-onnx Android `v1.13.6` |
| ABI inicial | `arm64-v8a` para o Samsung SM-S921B |
| Provider | CPU |
| Threads iniciais | 2 |
| Modelo local | `android/local-models/parakeet/` |
| JNI local | `android/local-native-libs/jniLibs/` |
| Política | ONNX, `.so`, ZIP, APK e modelos ficam fora do Git |

Os arquivos esperados do modelo são `encoder.int8.onnx`, `decoder.int8.onnx`, `joiner.int8.onnx` e `tokens.txt`. As bibliotecas JNI incluem `libonnxruntime.so`, `libsherpa-onnx-jni.so`, `libsherpa-onnx-c-api.so` e `libsherpa-onnx-cxx-api.so`.

## Como configurar

Na raiz do repositório:

```bash
git checkout feat/stt-conformer-rnnt-sherpa-onnx
git pull --ff-only origin feat/stt-conformer-rnnt-sherpa-onnx
bash android/scripts/fetch-sherpa-onnx-android.sh
bash android/scripts/fetch-parakeet-tdt-v3.sh
```

O primeiro script baixa e valida as bibliotecas Android do sherpa-onnx por SHA-256. O segundo baixa o pacote oficial de modelo Parakeet TDT v3 INT8, com aproximadamente 640 MB. Nenhum desses arquivos deve ser adicionado com `git add`.

Abra a pasta `android/` no Android Studio, sincronize o Gradle, execute **Build > Clean Project**, depois **Build > Assemble Project** e clique em **Run** no Samsung. O processamento é local e não depende de Internet depois que o modelo e o runtime foram instalados.

## Teste comparativo

Use as mesmas frases testadas na branch Vosk:

```text
Olá, eu sou Fernando.
Tetiruã, explique este monumento.
```

Registre a transcrição, o tempo após a gravação, o tempo da primeira execução, o tempo da segunda execução sem liberar o modelo e qualquer erro de memória. A comparação precisa considerar também que o Parakeet é muito maior que o Vosk pequeno.

## Limitações e riscos

O principal risco desta branch é o consumo de memória e o tempo de carregamento do encoder INT8, que tem centenas de megabytes. O fato de o catálogo listar `pt` não garante a mesma qualidade em português brasileiro observada em português europeu. O modelo também é offline, mas não é uma solução leve como Vosk.

VAD e wake word “Tetiruã” ainda não estão integrados. O sherpa-onnx possui APIs que poderão ser usadas em uma etapa posterior, mas esta branch inicial mede somente o STT para que o benchmark permaneça isolado.

## Referências

- [Parakeet TDT v3 no catálogo sherpa-onnx](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/nemo-transducer-models.html)
- [Android sherpa-onnx](https://k2-fsa.github.io/sherpa/onnx/android/)
- [API Kotlin oficial](https://github.com/k2-fsa/sherpa-onnx/tree/master/sherpa-onnx/kotlin-api)
- [Model card NVIDIA Parakeet TDT v3](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3)
- [Release das bibliotecas Android](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.6)
