# Tetiruã Audio Android

Este módulo é a primeira base Android/Kotlin da camada de áudio do Tetiruã. Ele separa a aplicação móvel dos adaptadores Python de pesquisa e mantém os contratos comuns de wake word, VAD, STT e TTS.

## O que já funciona no esqueleto

A `MainActivity` solicita permissão de microfone, simula a ativação da wake word e reproduz texto em português brasileiro com o `AndroidTtsEngine`. O teste demonstrativo não chama LLM, VLM, GPS ou web search; ele valida apenas o caminho de áudio local:

```text
wake_word.detected → vad.speech.started → stt.final (simulado) → tts.started → tts.completed
```

O TTS nativo não exige pesos adicionais. Para testar, abra a pasta `android/` no Android Studio, sincronize o Gradle e execute o aplicativo em um dispositivo Android ou emulador com um mecanismo de voz em português instalado.

## Situação dos motores

| Motor | Papel | Caminho Android/Kotlin | Estado deste módulo |
|---|---|---|---|
| Moonshine | STT | Binding/runtime nativo a validar | Catálogo e contrato preparados; adaptador específico será implementado na branch `feat/stt-moonshine`. |
| whisper.cpp | STT | JNI/binding Java/Kotlin e modelo local | Catálogo e contrato preparados; adaptador específico será implementado na branch `feat/stt-whisper-cpp-tflite`. |
| Android TTS | TTS | API nativa `android.speech.tts.TextToSpeech` | Adaptador funcional inicial em `tts/AndroidTtsEngine.kt`. |
| AVSpeechSynthesizer | TTS | Apenas iOS; não pertence ao módulo Android | Mantido na documentação multiplataforma. |
| Kokoro-82M | TTS | Runtime ONNX/sherpa-onnx, não Python direto | Catálogo e contrato preparados; runtime Android será validado na branch `feat/tts-kokoro-82m`. |
| Piper + sherpa-onnx | TTS | APIs Kotlin/Java e bibliotecas nativas por ABI | Catálogo e contrato preparados; integração será implementada na branch `feat/tts-piper-sherpa-onnx`. |
| Wake word/KWS | Ativação | `KeywordSpotter` via sherpa-onnx ou motor dedicado | Contrato preparado; motor definitivo será comparado separadamente. |
| VAD | Delimitação da fala | `Vad` via sherpa-onnx ou modelo compatível | Contrato preparado; simulador e integração real serão separados. |

## Modelos e bibliotecas

Pesos de modelos, arquivos ONNX, arquivos Piper, modelos Whisper, modelos Moonshine, vozes e bibliotecas nativas `.so` não devem ser enviados ao Git por padrão. O projeto deve versionar:

1. contratos e adaptadores Kotlin;
2. configuração de modelo;
3. scripts ou instruções de download;
4. hashes e versões esperadas;
5. testes sem pesos grandes;
6. documentação de licença e redistribuição.

Para sherpa-onnx, a documentação oficial descreve bibliotecas Android pré-compiladas ou build com NDK e arquivos nativos organizados por ABI. A integração inicial deverá registrar a versão exata utilizada e manter os binários em uma etapa de empacotamento, não misturados ao código-fonte.

## Execução

No Android Studio:

```text
Open -> /home/ubuntu/tetirua-guia/android
Sync Project with Gradle Files
Run app
```

Para usar a aplicação em dispositivo físico, habilite a depuração USB e aceite a permissão de microfone. Para testar o TTS, use o botão `Simular wake word e falar`. A etapa inicial não grava áudio nem envia dados para a internet.

## Próximas branches

A base comum deve ser levada para as cinco branches de solução, mas os adaptadores específicos devem ser desenvolvidos separadamente:

- `feat/stt-moonshine`;
- `feat/stt-whisper-cpp-tflite`;
- `feat/tts-native`;
- `feat/tts-kokoro-82m`;
- `feat/tts-piper-sherpa-onnx`.

Cada branch deverá conter teste, instrução de instalação, modelo compatível, benchmark e limitações. Nenhuma branch deve fazer merge na `main` antes da análise do Henrique e da comparação com as demais alternativas.
