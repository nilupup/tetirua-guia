# Engines Android independentes

## Regra arquitetural

Cada branch de solução deve gerar uma aplicação Android executável com um runtime próprio. O contrato do Tetiruã descreve a forma de trocar a pergunta e o resultado, mas não transforma os engines em plugins obrigatórios do mesmo APK.

```text
Branch Whisper      → aplicação com whisper.cpp/TFLite
Branch Moonshine    → aplicação com Moonshine
Branch Kokoro       → aplicação com Kokoro e runtime escolhido nessa branch
Branch Piper        → aplicação com Piper + sherpa-onnx nessa branch
Branch Native TTS   → aplicação com API nativa da plataforma
```

A implementação de uma branch não deve importar classes, bibliotecas nativas ou pesos de outra alternativa.

## Matriz de runtime

| Branch | Dependência permitida | O que não deve entrar |
|---|---|---|
| `feat/stt-whisper-cpp-tflite` | whisper.cpp, ggml, JNI/NDK ou TFLite escolhido para esta branch | sherpa-onnx, Moonshine, Kokoro e Piper. |
| `feat/stt-moonshine` | Moonshine e o binding/runtime que for validado para esta branch | whisper.cpp e sherpa-onnx como dependência de suporte. |
| `feat/tts-native` | API Android `TextToSpeech` ou módulo iOS correspondente | Pesos Kokoro/Piper e bibliotecas de inferência. |
| `feat/tts-kokoro-82m` | Kokoro-82M e o runtime Android escolhido exclusivamente para Kokoro | Piper, Whisper e Moonshine. |
| `feat/tts-piper-sherpa-onnx` | Piper e sherpa-onnx apenas nesta aplicação | sherpa-onnx como biblioteca compartilhada por outras branches. |

## Branch Whisper: implementação atual

A branch `feat/stt-whisper-cpp-tflite` usa o exemplo Android oficial do whisper.cpp como referência. O código upstream é mantido em `third_party/whisper.cpp` como submódulo. O módulo `android/whisper` compila a ponte JNI e expõe `WhisperContext` à camada Kotlin.

O fluxo desta branch é:

```text
AudioRecord
    → WAV PCM mono 16 kHz
    → WhisperContext via JNI
    → WhisperCppSttEngine
    → TranscriptionResult
```

O modelo deve ser fornecido pelo desenvolvedor em `android/app/src/main/assets/models/`. Ele não deve ser commitado.

## Troca de alternativa

Para testar outra alternativa, o colaborador deve abrir a branch correspondente, executar o procedimento de instalação documentado nela e comparar o resultado. Não deve instalar todos os runtimes na mesma aplicação para fazer uma comparação inicial.

A comparação entre branches deve usar o mesmo conjunto de gravações, o mesmo idioma, o mesmo fluxo de permissão e as mesmas métricas de latência, uso de memória, consumo de bateria, taxa de erro e qualidade. O fato de uma branch ser mais simples de integrar não determina que ela será escolhida para a aplicação final.

## VAD e wake word

VAD e wake word também devem seguir o princípio de independência. Se a branch Whisper utilizar VAD ou wake word do próprio runtime, esses componentes pertencem à branch Whisper. Se uma branch sherpa-onnx utilizar `Vad` e `KeywordSpotter`, eles pertencem apenas à aplicação sherpa-onnx. O contrato do orquestrador recebe eventos equivalentes, mas não exige que as implementações sejam iguais.

## Artefatos externos

Cada branch deve registrar seu próprio runtime, versão, ABI, instruções de build, URL dos modelos, hash, licença e procedimento de limpeza. Pesos, bibliotecas `.so`, arquivos `.aar`, caches e gravações pessoais permanecem fora do Git por padrão.
