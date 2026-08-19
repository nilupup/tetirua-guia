# Integração Parakeet TDT / sherpa-onnx no Android

## Separação conceitual

Esta branch contém três camadas relacionadas, mas independentes:

| Camada | Componente | Responsabilidade |
|---|---|---|
| Modelo | Parakeet TDT v3 INT8 | Pesos ONNX do FastConformer/TDT treinado para reconhecimento multilíngue. |
| Runtime | sherpa-onnx Android v1.13.6 | Executa encoder, decoder e joiner no CPU do telefone. |
| Adaptador | `ParakeetTdtSttEngine` | Converte WAV/PCM para a API Kotlin e retorna `TranscriptionResult`. |

O runtime sherpa-onnx é exclusivo desta branch. Não é uma dependência compartilhada com a branch Piper/TTS ou com Vosk.

## Arquivos nativos

O script `android/scripts/fetch-sherpa-onnx-android.sh` instala, em `android/local-native-libs/jniLibs`, os arquivos oficiais por ABI:

```text
arm64-v8a/libonnxruntime.so
arm64-v8a/libsherpa-onnx-jni.so
arm64-v8a/libsherpa-onnx-c-api.so
arm64-v8a/libsherpa-onnx-cxx-api.so
```

A versão inicial mantém também as outras ABIs no diretório local, mas o primeiro dispositivo-alvo é `arm64-v8a`. O Gradle inclui o diretório com:

```kotlin
sourceSets {
    getByName("main") {
        jniLibs.srcDir("../local-native-libs/jniLibs")
        assets.srcDir("../local-models/parakeet")
    }
}
```

Nenhum `.so` é versionado.

## API Kotlin

A branch mantém os arquivos oficiais necessários da API Kotlin do sherpa-onnx com o cabeçalho de licença upstream:

```text
com/k2fsa/sherpa/onnx/FeatureConfig.kt
com/k2fsa/sherpa/onnx/HomophoneReplacerConfig.kt
com/k2fsa/sherpa/onnx/OfflineRecognizer.kt
com/k2fsa/sherpa/onnx/OfflineStream.kt
com/k2fsa/sherpa/onnx/QnnConfig.kt
```

A configuração do Parakeet é:

```kotlin
OfflineRecognizerConfig(
    featConfig = FeatureConfig(sampleRate = 16_000),
    modelConfig = OfflineModelConfig(
        transducer = OfflineTransducerModelConfig(
            encoder = "$modelDir/encoder.int8.onnx",
            decoder = "$modelDir/decoder.int8.onnx",
            joiner = "$modelDir/joiner.int8.onnx",
        ),
        tokens = "$modelDir/tokens.txt",
        numThreads = 2,
        provider = "cpu",
        modelType = "nemo_transducer",
    ),
)
```

O áudio é convertido para `FloatArray` normalizado entre aproximadamente `-1.0` e `1.0`, enviado por `OfflineStream.acceptWaveform`, decodificado com `OfflineRecognizer.decode` e lido com `getResult`.

## Modelo Parakeet TDT v3

O modelo oficial do sherpa-onnx é baixado por `android/scripts/fetch-parakeet-tdt-v3.sh` e deve conter:

```text
sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8/
├── encoder.int8.onnx
├── decoder.int8.onnx
├── joiner.int8.onnx
├── tokens.txt
└── test_wavs/
```

O encoder é o maior arquivo, com aproximadamente 622 MB segundo a documentação do sherpa-onnx. O pacote inteiro tem aproximadamente 640 MB. Esses pesos ficam em `android/local-models/parakeet/`, são ignorados pelo Git e não entram em commits.

## Benchmark

A Activity grava seis segundos como baseline, mas mede separadamente o trecho após a captura. A primeira execução inclui a criação do `OfflineRecognizer` e o carregamento dos arquivos ONNX; a segunda execução, sem liberar o modelo, é a medida mais próxima do custo de inferência aquecida.

O benchmark precisa registrar transcrição, tempo de carregamento, tempo de inferência, tempo total, número de threads, memória e eventuais erros de alocação. A comparação direta com Vosk deve informar que o Parakeet usa aproximadamente 640 MB contra aproximadamente 31 MB do modelo Vosk pequeno.

## Próximas extensões

O runtime possui caminhos oficiais para VAD, reconhecimento streaming/simulated streaming e keyword spotting. Eles não são ativados nesta primeira implementação, pois o objetivo é comparar o STT puro. A wake word “Tetiruã” e o VAD serão adicionados posteriormente como componentes desta branch ou avaliados em branches próprias, conforme a decisão arquitetural do projeto.

## Referências

- [Documentação oficial do modelo Parakeet TDT v3](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/nemo-transducer-models.html)
- [Documentação Android sherpa-onnx](https://k2-fsa.github.io/sherpa/onnx/android/)
- [API Kotlin oficial](https://github.com/k2-fsa/sherpa-onnx/tree/master/sherpa-onnx/kotlin-api)
- [Repositório Android oficial](https://github.com/k2-fsa/sherpa-onnx/tree/master/android)
