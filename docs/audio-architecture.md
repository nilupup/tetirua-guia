# Arquitetura inicial de STT e TTS

## Objetivo

A camada de áudio do Tetiruã precisa receber perguntas faladas pelo usuário, entregar texto ao orquestrador e converter a resposta do LLM em áudio para reprodução no celular ou nos dispositivos conectados. Como o sistema deverá evoluir entre PC, celular e óculos, os módulos não devem depender diretamente de um único modelo.

A implementação inicial adota **interfaces estáveis e adaptadores substituíveis**. O contrato de STT normaliza uma transcrição em `TranscriptionResult`, enquanto o contrato de TTS normaliza o áudio produzido em `SynthesisResult`. Dessa forma, a camada de orquestração pode trabalhar com os contratos sem conhecer detalhes de Moonshine, whisper.cpp, Kokoro ou sherpa-onnx.

## Estado do primeiro protótipo

| Camada | Implementação inicial | Papel | Estado |
|---|---|---|---|
| STT baseado em arquivo | `WhisperCppSTT` | Executa `whisper-cli` localmente e interpreta segmentos com timestamps. | Implementado |
| STT streaming | `MoonshineMicSTT` | Adaptador de microfone com callbacks de texto parcial e linha final. | Implementado como opcional |
| TTS local no PC | `KokoroTTS` | Gera WAV em português brasileiro com Kokoro-82M. | Implementado como opcional |
| TTS no Android | Android TTS ou sherpa-onnx | Reproduz a resposta localmente no celular. | Próxima etapa |

## Decisões técnicas

### STT

O repositório `whisper.cpp` oferece uma implementação local em C/C++, modelos quantizados e exemplos para Android. O adaptador `WhisperCppSTT` não compila o projeto nem baixa modelos automaticamente: recebe o caminho do executável e do modelo, executa a inferência e devolve um resultado uniforme. Essa separação evita colocar binários e modelos grandes no Git.

O Moonshine foi incluído como adaptador de streaming porque sua API oferece callbacks de texto parcial e de linhas finalizadas. Entretanto, o catálogo oficial consultado lista modelos para vários idiomas, mas não lista português brasileiro. Por isso, o módulo está disponível para testes e idiomas compatíveis, mas não é a escolha padrão do STT em português até que um modelo adequado seja validado.

### TTS

O Kokoro foi selecionado para o protótipo Python porque a biblioteca declara suporte a português brasileiro com `lang_code='p'` e as vozes `pf_dora`, `pm_alex` e `pm_santa`. O adaptador usa `pf_dora` por padrão, permite troca de voz e gera WAV a 24 kHz. As dependências são carregadas de forma tardia, então importar o pacote não exige instalar PyTorch, `soundfile` ou `espeak-ng`.

Para Android, sherpa-onnx é o caminho de integração mais abrangente entre as referências, pois disponibiliza APIs e exemplos para Android, Kotlin, Java e Flutter e oferece engines de TTS com modelos locais. A licença do framework e a licença de cada modelo precisam ser verificadas separadamente antes de empacotar uma versão distribuível.

## Fluxo pretendido

```text
Microfone do celular/óculos
          |
          v
   STT adapter (texto)
          |
          v
   Orquestrador / LLM
          |
          v
   TTS adapter (WAV/stream)
          |
          v
Reprodução no celular ou dispositivo conectado
```

O MVP deve manter a comunicação entre os módulos baseada em texto e arquivos/streams de áudio simples. A integração com o LLM, GPS, VLM e web search será feita em uma camada superior; os adaptadores de áudio não devem conter lógica de busca ou contexto geográfico.

## Instalação opcional

O pacote principal não instala modelos nem dependências pesadas:

```bash
python -m pip install -e .
```

Para experimentar Kokoro no PC, instalar a opção TTS e o `espeak-ng` de acordo com o sistema operacional:

```bash
python -m pip install -e '.[tts]'
```

Para experimentar o adaptador de microfone Moonshine:

```bash
python -m pip install -e '.[stt-moonshine]'
```

O `whisper.cpp` deve ser compilado externamente seguindo a documentação oficial, com um modelo compatível disponível localmente. Modelos, binários, arquivos de áudio de teste e caches devem permanecer fora do repositório ou ser baixados por scripts documentados.

## Exemplo de uso

```python
from stt import WhisperCppSTT
from tts import KokoroTTS

stt = WhisperCppSTT(
    executable="/caminho/para/whisper-cli",
    model_path="/caminho/para/ggml-base.bin",
)
transcription = stt.transcribe("pergunta.wav")
print(transcription.text)

tts = KokoroTTS(voice="pf_dora")
audio = tts.synthesize(
    "Este é um teste de resposta falada do Tetiruã.",
    "artifacts/resposta.wav",
)
print(audio.audio_path)
```

## Próximos passos

A próxima etapa deve validar o STT em português com um modelo multilíngue do Whisper ou com um modelo compatível com sherpa-onnx. Em seguida, deve ser criado um pequeno cliente Android que receba a transcrição, envie a pergunta ao orquestrador e reproduza o resultado usando Android TTS como fallback. Depois que o fluxo estiver funcional, Kokoro ou um modelo TTS empacotado via sherpa-onnx poderá substituir o fallback, caso a latência, a qualidade e o tamanho do modelo sejam aceitáveis.

## Referências

1. [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
2. [Modelos whisper.cpp no Hugging Face](https://huggingface.co/ggerganov/whisper.cpp)
3. [Moonshine Voice](https://github.com/moonshine-ai/moonshine)
4. [Documentação de classes do Moonshine](https://moonshine-voice.readthedocs.io/en/latest/api/classes/)
5. [Modelos disponíveis do Moonshine](https://moonshine-voice.readthedocs.io/en/latest/models/available-models/)
6. [Kokoro](https://github.com/hexgrad/kokoro)
7. [Vozes do Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md)
8. [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
9. [Engine TTS Android do sherpa-onnx](https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html)
