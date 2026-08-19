# Tetiruã Audio Android — branch Moonshine

Esta branch é uma aplicação Android/Kotlin independente para testar **Moonshine Voice** como runtime de STT. Ela mantém os contratos comuns de wake word, VAD, STT e TTS do Tetiruã, mas não inclui Whisper.cpp, sherpa-onnx, Kokoro ou Piper.

## O que a aplicação demonstra

A `MainActivity` carrega o modelo oficial Moonshine no primeiro uso, inicia o microfone, mostra transcrições parciais na tela e finaliza uma transcrição ao pressionar o botão correspondente. O fluxo é:

```text
microfone → Moonshine MicTranscriber → texto parcial/final na tela
```

A Activity não chama LLM, VLM, GPS ou web search. Esses componentes permanecem responsabilidade das outras equipes e podem consumir o `TranscriptionResult` pelo contrato do Tetiruã.

## Estado dos motores nesta branch

| Motor | Papel | Estado nesta branch |
|---|---|---|
| Moonshine Voice | STT | Implementado com `ai.moonshine:moonshine-voice:0.1.3`. |
| Whisper.cpp | STT | Ausente; pertence exclusivamente à branch `feat/stt-whisper-cpp-tflite`. |
| Android TTS | TTS | Mantido apenas como contrato/base; não é usado pela Activity desta branch. |
| Kokoro-82M | TTS | Ausente; pertence exclusivamente à branch `feat/tts-kokoro-82m`. |
| Piper + sherpa-onnx | TTS/VAD/KWS | Ausente; pertence exclusivamente à branch `feat/tts-piper-sherpa-onnx`. |

## Limitação de idioma

A documentação oficial consultada em 18 de agosto de 2026 lista modelos para árabe, inglês, espanhol, japonês, coreano, mandarim, ucraniano e vietnamita. **Português brasileiro não está listado na versão utilizada**, portanto a demonstração é configurada para inglês e não deve ser apresentada como STT pt-BR.

Referências oficiais: [Moonshine Voice](https://github.com/moonshine-ai/moonshine), [Quickstart Android](https://moonshine-voice.readthedocs.io/en/latest/quickstart/), [modelos disponíveis](https://moonshine-voice.readthedocs.io/en/latest/models/available-models/) e [artefato Maven](https://central.sonatype.com/artifact/ai.moonshine/moonshine-voice).

## Execução

Abra `android/` no Android Studio, sincronize o Gradle e execute o módulo `app` em um dispositivo Android ou emulador com microfone. O primeiro carregamento pode baixar o modelo para o cache gerenciado pelo SDK. O projeto não versiona os arquivos de modelo.

O build exige Android SDK, NDK/CMake somente se uma dependência nativa os solicitar, e a versão de Gradle indicada pelo Wrapper. Se necessário, copie `android/local.properties.example` para `android/local.properties` e ajuste `sdk.dir` para o caminho local do Android SDK. Não versione `local.properties`.

## Contrato e branch strategy

O adaptador `MoonshineSttEngine` implementa `SpeechToTextEngine` e também expõe `startListening()`/`stopListening()` para a Activity demonstrativa. O resultado contém IDs de sessão/turno/requisição, texto final, idioma, duração, engine, modelo e avisos.

A regra de troca permanece: para testar outro runtime, troque de branch; não instale todos os runtimes na mesma aplicação. Nenhuma branch deve ser mesclada à `main` antes da análise do Henrique.
