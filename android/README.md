# Tetiruã Audio Android — branch Android TTS nativo

Esta branch é uma aplicação Android/Kotlin independente para testar a API de plataforma `android.speech.tts.TextToSpeech`. Ela não usa pesos neurais, sherpa-onnx, Whisper.cpp, Moonshine, Kokoro ou Piper.

## O que a aplicação demonstra

A `MainActivity` recebe o texto produzido pelo orquestrador, solicita voz `pt-BR`, reproduz a resposta no alto-falante ou sintetiza um WAV no cache e exibe na tela o engine, idioma, arquivo e estado:

```text
texto do orquestrador → Android TextToSpeech → playback ou WAV
```

A branch não pede permissão de microfone porque sua responsabilidade é somente TTS. Captura STT, wake word e VAD continuam sendo avaliadas nas branches próprias.

## Dependência de voz

A qualidade e a disponibilidade do português dependem do mecanismo TTS instalado no aparelho ou emulador. Se o dispositivo não tiver dados de voz pt-BR, o adaptador devolve um aviso `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` no `SynthesisResult`; a aplicação não baixa vozes automaticamente.

## Execução

Abra `android/` no Android Studio, copie `android/local.properties.example` para `android/local.properties` se necessário, ajuste o caminho do SDK e execute o módulo `app`. Em um dispositivo físico, instale ou habilite uma voz brasileira nas configurações de síntese de fala do Android antes do teste.

Esta branch não precisa de scripts de modelo, bibliotecas `.so`, NDK ou assets externos. O build remoto continua limitado pela ausência de Android SDK no ambiente de desenvolvimento desta tarefa; a confirmação final do APK deve ser feita em uma máquina com Android Studio.

## Estrutura

| Arquivo | Responsabilidade |
|---|---|
| `app/src/main/java/br/com/tetirua/audio/tts/AndroidTtsEngine.kt` | Adaptador da API nativa, seleção de idioma/voz, playback e `synthesizeToFile`. |
| `app/src/main/java/br/com/tetirua/audio/MainActivity.kt` | Tela independente de playback e geração de WAV. |
| `app/src/main/AndroidManifest.xml` | Activity sem permissão de microfone. |

## Referências

A implementação usa a documentação oficial de [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener) e [synthesizeToFile](https://developer.android.com/reference/android/speech/tts/TextToSpeech#synthesizeToFile(java.lang.CharSequence,%20android.os.Bundle,%20java.io.File,%20java.lang.String)).

## Independência de branches

Para testar Piper, Kokoro, Moonshine ou Whisper, troque para a branch correspondente. Esta branch não deve receber bibliotecas nativas ou modelos dessas alternativas e não deve ser mesclada à `main` sem aprovação do Henrique.
