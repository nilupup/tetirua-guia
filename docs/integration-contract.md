# Contrato de integração do Tetiruã

## 1. Objetivo

Este documento define como a camada de áudio, formada por **STT** e **TTS**, se comunica com o orquestrador e com as equipes de **LLM**, **VLM**, localização e web search. O contrato existe para que cada equipe possa desenvolver e testar seu componente separadamente, sem depender da implementação interna das outras.

A regra principal é a seguinte:

> O STT conhece áudio e devolve texto. O TTS conhece texto e devolve áudio. O orquestrador conhece a interação e coordena STT, VLM, GPS, web search e LLM.

Nenhum modelo específico — Moonshine, whisper.cpp/TFLite, Android TTS/AVSpeechSynthesizer, Kokoro ou Piper+sherpa-onnx — deve aparecer como dependência obrigatória no formato das mensagens. O motor escolhido entra apenas na configuração e nos metadados.

## 2. Componentes e responsabilidades

| Componente | Responsabilidade | Não deve assumir |
|---|---|---|
| Captura no celular/óculos | Capturar áudio, imagem e localização; controlar permissões e ciclo de vida do hardware. | Como o STT interpreta o áudio ou como o LLM formula a resposta. |
| STT | Converter fala em texto, de modo parcial ou final, e informar metadados de transcrição. | Identificar monumentos, consultar a web ou gerar respostas. |
| Orquestrador | Criar a interação, correlacionar eventos, reunir contexto e chamar as outras equipes. | Implementar internamente cada modelo de STT, TTS, LLM ou VLM. |
| VLM | Interpretar a imagem e produzir descrição, objetos, candidatos ou atributos visuais. | Transcrever a pergunta ou sintetizar áudio. |
| GPS/localização | Fornecer coordenadas e contexto geográfico com nível de precisão. | Decidir qual monumento foi identificado ou qual resposta é correta. |
| Web search | Recuperar fontes, trechos, títulos e URLs para complementar a resposta. | Falar diretamente com o usuário ou substituir a síntese do TTS. |
| LLM | Interpretar pergunta e contexto, produzir resposta e organizar fontes. | Acessar diretamente microfone, câmera ou modelos de áudio. |
| TTS | Converter a resposta textual em áudio, por arquivo ou stream. | Fazer buscas, interpretar imagens ou alterar o conteúdo factual da resposta. |
| Aplicação móvel | Exibir texto, reproduzir áudio, permitir follow-up e selecionar configurações. | Acoplar o orquestrador a um único motor de áudio. |

## 3. Identificadores de correlação

Todas as mensagens devem carregar identificadores para que uma resposta de áudio possa ser relacionada à pergunta e à imagem que a originaram.

| Identificador | Escopo | Exemplo |
|---|---|---|
| `session_id` | Sessão contínua do usuário, podendo conter várias perguntas. | `sess_01H...` |
| `turn_id` | Uma pergunta e sua resposta. | `turn_0007` |
| `request_id` | Uma chamada específica entre dois componentes. | `req_01H...` |
| `scene_id` | Uma cena observada, que pode ser reutilizada em follow-ups. | `scene_0003` |
| `event_id` | Um evento individual do fluxo. | `evt_01H...` |

O `session_id` permanece estável durante a conversa. O `turn_id` muda a cada nova pergunta. O `request_id` pode mudar entre STT, VLM, busca, LLM e TTS, enquanto `session_id`, `turn_id` e `scene_id` permitem reconstruir a interação inteira.

## 4. Envelope comum de mensagens e eventos

Mensagens assíncronas devem usar um envelope comum:

```json
{
  "schema_version": "1.0",
  "event_id": "evt_01HXYZ",
  "event_type": "stt.final",
  "timestamp": "2026-08-18T23:30:00Z",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "request_id": "req_01HXYZ",
  "source": "stt",
  "payload": {}
}
```

`schema_version` permite evoluir o contrato sem quebrar versões antigas. `event_type` identifica a mensagem. `source` identifica o componente emissor, não o modelo comercial utilizado.

## 5. Contrato de entrada do STT

O orquestrador ou a aplicação envia áudio ao adaptador STT por arquivo, buffer ou stream. A camada de transporte pode variar entre PC, Android e outros dispositivos, mas o conteúdo lógico deve permanecer equivalente.

```json
{
  "schema_version": "1.0",
  "request_id": "req_stt_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "source": {
    "device": "android",
    "input": "microphone",
    "mode": "push_to_talk"
  },
  "audio": {
    "encoding": "pcm_s16le",
    "sample_rate_hz": 16000,
    "channels": 1,
    "path": "artifacts/turn_0007.wav",
    "duration_ms": 2450
  },
  "language_hint": "pt-BR",
  "streaming": false
}
```

O adaptador STT deve aceitar, quando a plataforma permitir, arquivos e streaming. O formato mínimo de referência para testes locais é PCM assinado de 16 bits, mono, com taxa de 16 kHz. Um adaptador pode aceitar outros formatos, mas deve normalizá-los ou informar claramente a incompatibilidade.

### 5.1 Resultado final do STT

```json
{
  "schema_version": "1.0",
  "request_id": "req_stt_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "result": {
    "text": "Quem projetou este prédio?",
    "language": "pt-BR",
    "is_final": true,
    "confidence": null,
    "segments": [],
    "audio_duration_ms": 2450,
    "processing_time_ms": 420
  },
  "engine": {
    "name": "whisper-cpp",
    "model": "base",
    "runtime": "whisper.cpp",
    "device": "android"
  },
  "warnings": []
}
```

A confiança pode ser `null` quando o motor não a fornecer. Não devemos inventar uma pontuação. O texto final é o dado consumido pelo orquestrador; os demais campos servem para diagnóstico, benchmark e observabilidade.

## 6. Contrato de saída do STT para o orquestrador

O resultado normalizado mínimo é:

```text
TranscriptionResult
- text: string
- language: string | null
- is_final: boolean
- confidence: number | null
- segments: lista opcional
- audio_duration_ms: integer | null
- processing_time_ms: integer | null
- engine: string
- model: string | null
- metadata: objeto
```

O orquestrador não deve importar classes específicas de Moonshine ou whisper.cpp. Ele deve consumir uma estrutura equivalente a `TranscriptionResult`, seja por chamada Python, HTTP, IPC, fila ou evento móvel.

## 7. Contexto enviado ao VLM, GPS e web search

Depois de receber `stt.final`, o orquestrador cria o contexto multimodal. O áudio não precisa ser repassado ao VLM ou ao LLM; em geral, o texto final é suficiente para a pergunta.

```json
{
  "schema_version": "1.0",
  "request_id": "req_context_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "scene": {
    "scene_id": "scene_0003",
    "image_uri": "artifacts/frame_0003.jpg",
    "captured_at": "2026-08-18T23:29:57Z",
    "description": null,
    "objects": [],
    "candidates": []
  },
  "location": {
    "latitude": -23.5505,
    "longitude": -46.6333,
    "accuracy_m": 12.0,
    "city": "São Paulo",
    "neighborhood": "Centro",
    "street": null
  },
  "question": {
    "text": "Quem projetou este prédio?",
    "language": "pt-BR",
    "source": "stt"
  },
  "conversation": {
    "previous_subject": null,
    "follow_up_context": null
  }
}
```

O VLM preenche a descrição da cena e candidatos visuais. O serviço de localização preenche coordenadas e precisão. O web search recebe uma consulta construída pelo orquestrador e devolve fontes estruturadas. Nenhum desses componentes deve alterar o texto original da pergunta sem registrar a transformação.

## 8. Contrato do resultado do LLM

O LLM devolve uma resposta textual estruturada. É recomendável separar o texto exibido na tela do texto falado, pois fontes, URLs e detalhes longos podem ser adequados para a tela, mas inadequados para áudio.

```json
{
  "schema_version": "1.0",
  "request_id": "req_llm_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "answer": {
    "display_text": "O prédio foi projetado por ...",
    "spoken_text": "Este prédio foi projetado por ...",
    "language": "pt-BR",
    "sources": [
      {
        "title": "Fonte histórica",
        "url": "https://exemplo.org/fonte",
        "snippet": "Trecho usado para fundamentar a resposta."
      }
    ]
  },
  "follow_up_context": {
    "scene_id": "scene_0003",
    "subject": "prédio observado",
    "facts_used": ["autor do projeto"],
    "location_label": "São Paulo, Centro"
  }
}
```

`spoken_text` é opcional. Quando não existir, o TTS usa `display_text`. O LLM não deve incluir instruções de síntese dentro do texto factual. Preferências como velocidade, voz e volume pertencem à configuração do TTS ou à aplicação móvel.

## 9. Contrato de entrada do TTS

O TTS recebe apenas o texto que deve ser falado e uma configuração de apresentação. Ele não precisa conhecer a imagem, o GPS, as fontes ou a implementação do LLM.

```json
{
  "schema_version": "1.0",
  "request_id": "req_tts_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "text": "Este prédio foi projetado por ...",
  "language": "pt-BR",
  "voice": "pf_dora",
  "speed": 1.0,
  "output": {
    "mode": "stream",
    "format": "pcm_s16le",
    "sample_rate_hz": 24000,
    "channels": 1,
    "path": null
  }
}
```

Para um protótipo por arquivo, `mode` pode ser `file` e `path` pode apontar para um WAV. Para a aplicação móvel, `mode` pode ser `stream`, permitindo iniciar a reprodução antes de a resposta completa terminar.

## 10. Resultado do TTS

```json
{
  "schema_version": "1.0",
  "request_id": "req_tts_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "result": {
    "audio_uri": "artifacts/turn_0007.wav",
    "format": "wav",
    "sample_rate_hz": 24000,
    "channels": 1,
    "duration_ms": 3100,
    "first_audio_ms": 260,
    "processing_time_ms": 740
  },
  "engine": {
    "name": "kokoro",
    "model": "Kokoro-82M",
    "voice": "pf_dora",
    "runtime": "python",
    "device": "pc"
  },
  "warnings": []
}
```

O resultado pode carregar `audio_uri`, bytes, identificador de stream ou callback, conforme a plataforma. O contrato lógico é o mesmo: o orquestrador precisa saber se o áudio está pronto, como reproduzi-lo e quais metadados foram usados.

## 11. Eventos assíncronos

A implementação pode funcionar por chamadas síncronas no primeiro protótipo, mas deve permitir eventos para streaming e baixa latência.

| Evento | Emissor | Campos importantes | Ação do receptor |
|---|---|---|---|
| `stt.started` | STT | `request_id`, `engine` | Marcar transcrição como iniciada. |
| `stt.partial` | STT | `text`, `sequence`, `is_final=false` | Atualizar texto provisório, sem chamar o LLM ainda. |
| `stt.final` | STT | `text`, `language`, `is_final=true` | Encerrar captura e iniciar contexto/orquestração. |
| `context.ready` | Orquestrador | `scene`, `location`, `question` | Enviar contexto ao LLM ou à busca. |
| `llm.started` | Orquestrador/LLM | `request_id`, `turn_id` | Exibir estado de processamento. |
| `llm.answer.ready` | LLM | `display_text`, `spoken_text`, `sources` | Exibir resposta e solicitar TTS. |
| `tts.started` | TTS | `engine`, `voice` | Preparar reprodução. |
| `tts.chunk` | TTS | `sequence`, `audio_uri` ou bytes | Reproduzir trecho recebido. |
| `tts.completed` | TTS | `duration_ms`, `processing_time_ms` | Marcar áudio como concluído. |
| `interaction.completed` | Orquestrador | `turn_id`, `status` | Fechar o turno e preservar contexto. |
| `audio.error` | STT/TTS | `code`, `message`, `retryable` | Aplicar fallback ou informar falha. |

Um evento `stt.partial` nunca deve iniciar uma busca ou gerar uma resposta final. O orquestrador aguarda `stt.final`, salvo se uma modalidade futura for explicitamente desenhada para antecipar a resposta.

## 12. Máquina de estados do turno

O ciclo normal de uma pergunta é:

```text
IDLE
  -> CAPTURING
  -> TRANSCRIBING
  -> CONTEXT_BUILDING
  -> SEARCHING / ANALYZING
  -> GENERATING
  -> SYNTHESIZING
  -> PLAYING
  -> COMPLETED
```

O cancelamento pode ocorrer em qualquer estado:

```text
qualquer estado -> CANCELLING -> CANCELLED
```

Falhas entram em `ERROR`, levando a uma tentativa de fallback, repetição ou encerramento:

```text
qualquer estado -> ERROR -> FALLBACK ou FAILED
```

| Estado | Responsável principal | Critério de saída |
|---|---|---|
| `CAPTURING` | Aplicação/áudio | Usuário solta o botão, silêncio detectado ou timeout. |
| `TRANSCRIBING` | STT | Texto final disponível ou erro de transcrição. |
| `CONTEXT_BUILDING` | Orquestrador/VLM/GPS | Contexto visual e geográfico pronto. |
| `SEARCHING` | Web search | Fontes retornadas ou busca encerrada com aviso. |
| `GENERATING` | LLM | Resposta textual validada. |
| `SYNTHESIZING` | TTS | Áudio ou primeiro chunk disponível. |
| `PLAYING` | Aplicação móvel | Reprodução concluída, interrompida ou cancelada. |
| `COMPLETED` | Orquestrador | Turno armazenado para follow-up. |

## 13. Erros normalizados

Erros devem ter código estável, mensagem legível, indicação de repetição e, quando possível, uma alternativa.

```json
{
  "schema_version": "1.0",
  "event_type": "audio.error",
  "request_id": "req_tts_0007",
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0007",
  "error": {
    "code": "MODEL_NOT_INSTALLED",
    "message": "O modelo Kokoro não está instalado neste dispositivo.",
    "component": "tts",
    "engine": "kokoro",
    "retryable": false,
    "fallback_engine": "android-tts",
    "details": {}
  }
}
```

| Código | Componente | Tratamento esperado |
|---|---|---|
| `AUDIO_PERMISSION_DENIED` | Aplicação | Pedir permissão ou encerrar o turno. |
| `AUDIO_FORMAT_UNSUPPORTED` | STT/TTS | Converter formato ou informar incompatibilidade. |
| `MODEL_NOT_INSTALLED` | STT/TTS | Selecionar outro motor ou orientar instalação. |
| `UNSUPPORTED_LANGUAGE` | STT/TTS | Tentar outra alternativa ou informar o idioma disponível. |
| `STT_TIMEOUT` | STT | Repetir, reduzir janela ou pedir nova fala. |
| `EMPTY_TRANSCRIPT` | STT | Pedir que o usuário repita. |
| `VLM_UNAVAILABLE` | VLM | Continuar apenas com localização/pergunta, se possível. |
| `SEARCH_UNAVAILABLE` | Web search | Responder com contexto local disponível e avisar ausência de fontes. |
| `LLM_TIMEOUT` | LLM | Repetir ou mostrar mensagem de indisponibilidade. |
| `TTS_UNAVAILABLE` | TTS | Tentar outro TTS ou exibir apenas texto. |
| `CANCELLED` | Qualquer | Liberar recursos e encerrar o turno sem erro fatal. |

## 14. Follow-up e memória da conversa

A primeira pergunta pode identificar uma cena. Uma pergunta seguinte, como “E quem construiu?”, não precisa enviar uma nova imagem se o usuário ainda estiver olhando para o mesmo local. O aplicativo reenvia o `session_id`, cria um novo `turn_id` e referencia o `scene_id` e o `follow_up_context` anteriores.

```json
{
  "session_id": "sess_01HXYZ",
  "turn_id": "turn_0008",
  "question": {
    "text": "E quem construiu?",
    "language": "pt-BR",
    "source": "stt"
  },
  "conversation": {
    "previous_turn_id": "turn_0007",
    "scene_id": "scene_0003",
    "subject": "prédio observado",
    "facts_used": ["autor do projeto"]
  }
}
```

O orquestrador decide se precisa capturar uma nova imagem ou consultar novamente o VLM. STT e TTS continuam sem conhecer a memória semântica da conversa.

## 15. Exemplo ponta a ponta

A interação completa ocorre da seguinte forma:

| Passo | Emissor | Mensagem | Resultado |
|---:|---|---|---|
| 1 | Aplicação | `audio.input` | Envia áudio e `turn_id` ao STT. |
| 2 | STT | `stt.partial` | Mostra transcrição provisória, sem acionar resposta. |
| 3 | STT | `stt.final` | Entrega a pergunta final ao orquestrador. |
| 4 | Orquestrador | Solicitações ao VLM/GPS | Obtém descrição visual e localização. |
| 5 | Orquestrador | Consulta ao web search | Recupera fontes sobre o provável local. |
| 6 | Orquestrador | Solicitação ao LLM | Envia pergunta, imagem interpretada, localização e fontes. |
| 7 | LLM | `llm.answer.ready` | Entrega `display_text`, `spoken_text` e fontes. |
| 8 | Orquestrador | `tts.request` | Solicita áudio ao motor escolhido. |
| 9 | TTS | `tts.started`/`tts.chunk` | A aplicação começa a reproduzir. |
| 10 | TTS | `tts.completed` | Informa fim e métricas do áudio. |
| 11 | Aplicação | Tela + áudio | Exibe texto/fontes e reproduz a resposta. |
| 12 | Orquestrador | `interaction.completed` | Guarda o contexto para follow-up. |

## 16. Seleção de motor

A escolha do motor é configuração, não contrato de negócio:

```text
STT_ENGINE=whisper-cpp
STT_MODEL=base
TTS_ENGINE=kokoro
TTS_VOICE=pf_dora
AUDIO_LANGUAGE=pt-BR
```

As configurações equivalentes podem selecionar Moonshine, whisper.cpp/TFLite, Android TTS/AVSpeechSynthesizer, Kokoro ou Piper+sherpa-onnx. O restante da aplicação continua usando os mesmos campos normalizados.

## 17. Compatibilidade, testes e segurança

Cada implementação deve declarar a versão do contrato que suporta e executar testes de contrato com entradas e saídas artificiais. Um teste de contrato não precisa baixar um modelo grande: ele verifica campos obrigatórios, estados, tratamento de texto vazio, erro de dependência, cancelamento e propagação de metadados.

O áudio bruto deve permanecer local por padrão. O orquestrador deve receber texto e metadados, não o arquivo de áudio, salvo quando houver uma necessidade explícita de auditoria ou reprocessamento. Caminhos locais não devem ser enviados a serviços remotos como se fossem URLs públicas. Tokens, modelos grandes, áudios pessoais e dados de localização sensíveis não devem ser versionados no GitHub.

A versão inicial do contrato é `1.0`. Alterações compatíveis podem adicionar campos opcionais. Alterações que mudem significado, removam campos ou alterem estados devem criar uma nova versão e ser discutidas entre as equipes.
