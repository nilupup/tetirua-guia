# Integração dos engines no Android — TTS nativo

## Princípio

Esta branch testa somente o TTS fornecido pelo Android. Os contratos em `core/` permanecem compatíveis com o orquestrador, mas nenhum runtime neural externo é instalado.

| Componente | Implementação desta branch | Observação |
|---|---|---|
| TTS | `android.speech.tts.TextToSpeech` | API da plataforma, sem pesos e sem `.so`. |
| Idioma | `Locale.forLanguageTag("pt-BR")` | Depende dos dados de voz instalados no dispositivo. |
| Playback | `TextToSpeech.speak()` | Usa o mecanismo padrão configurado no aparelho. |
| Arquivo | `TextToSpeech.synthesizeToFile()` | WAV/arquivo gerado no caminho informado. |
| STT | Ausente nesta branch | Testado nas branches Whisper e Moonshine. |
| Wake word/VAD | Contratos apenas | Implementação futura em branch específica. |

## Contrato

`AndroidTtsEngine` implementa `TextToSpeechEngine` e retorna `SynthesisResult` com `engine = "android-tts"`, idioma, voz solicitada, caminho de arquivo quando aplicável, duração aproximada e avisos de idioma ausente ou não suportado.

## Troca de alternativa

Para comparar com Piper ou Kokoro, troque de branch. Não adicione sherpa-onnx ou bibliotecas nativas nesta branch; o objetivo é manter uma linha de base simples, barata e dependente somente do sistema operacional.

## Referências

- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener)
- [synthesizeToFile](https://developer.android.com/reference/android/speech/tts/TextToSpeech#synthesizeToFile(java.lang.CharSequence,%20android.os.Bundle,%20java.io.File,%20java.lang.String))
