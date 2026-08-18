# Contrato de integração do Tetiruã

## Princípio

A camada de áudio deve funcionar independentemente de câmera, VLM, LLM, GPS e web search. Esses componentes podem ser desenvolvidos por equipes diferentes e substituídos sem alterar os adaptadores de STT e TTS.

## Entrada do orquestrador

O orquestrador pode receber uma pergunta já transcrita ou solicitar a transcrição a um adaptador de STT. Quando houver contexto visual e geográfico, os dados devem ser representados de forma explícita:

```json
{
  "request_id": "uuid",
  "question": {
    "text": "Quem projetou este prédio?",
    "language": "pt-BR",
    "source": "stt"
  },
  "scene": {
    "image_path": "artifacts/frame.jpg",
    "description": "Descrição produzida pelo VLM",
    "objects": ["prédio", "placa"]
  },
  "location": {
    "latitude": -23.5505,
    "longitude": -46.6333,
    "city": "São Paulo",
    "neighborhood": "Centro"
  },
  "search": {
    "sources": [],
    "query": null
  }
}
```

Nenhum adaptador de STT ou TTS deve depender da presença de `scene`, `location` ou `search`. Esses campos pertencem ao orquestrador.

## Saída do orquestrador

O LLM pode devolver uma resposta textual com fontes e metadados opcionais:

```json
{
  "request_id": "uuid",
  "answer": {
    "text": "O prédio foi projetado por ...",
    "language": "pt-BR",
    "sources": [
      {
        "title": "Fonte histórica",
        "url": "https://exemplo.org/fonte"
      }
    ]
  },
  "follow_up_context": {
    "subject": "prédio observado",
    "location": "São Paulo, Centro"
  }
}
```

O adaptador de TTS recebe apenas `answer.text`, a configuração de voz e o destino de áudio. As fontes podem ser exibidas na tela do celular, mas não precisam ser faladas integralmente.

## Eventos de áudio

Para streaming e conversa, as implementações podem produzir eventos normalizados:

| Evento | Campos mínimos | Significado |
|---|---|---|
| `stt.partial` | `request_id`, `text`, `sequence` | Texto parcial enquanto o usuário fala. |
| `stt.final` | `request_id`, `text`, `language` | Transcrição final pronta para o orquestrador. |
| `tts.started` | `request_id`, `audio_path` ou stream | A síntese ou reprodução começou. |
| `tts.completed` | `request_id`, `duration_ms` | O áudio foi produzido ou reproduzido. |
| `audio.error` | `request_id`, `engine`, `message` | O motor falhou e o orquestrador deve decidir se tenta outra alternativa. |

Os nomes dos motores devem aparecer nos metadados para permitir comparação e diagnóstico, mas não devem ser usados pelo LLM ou VLM como dependência de negócio.

## Troca de motor

O orquestrador deve selecionar uma implementação por configuração, por exemplo:

```text
STT_ENGINE=whisper-cpp
STT_MODEL=base
TTS_ENGINE=kokoro
TTS_VOICE=pf_dora
```

A mesma solicitação deve poder ser executada com Moonshine, whisper.cpp/TFLite, TTS nativo, Kokoro ou Piper+sherpa-onnx sem alterar o formato da pergunta ou da resposta. Quando uma alternativa não estiver instalada no dispositivo, o erro deve indicar a dependência ausente e permitir que a aplicação escolha outro motor configurado.

## Fronteiras de responsabilidade

O VLM descreve a imagem; o GPS informa o contexto geográfico; a busca recupera fontes; o LLM formula a resposta; o STT transcreve a pergunta; e o TTS sintetiza a resposta. A aplicação móvel controla permissões, ciclo de vida do microfone, reprodução, tela e seleção de configuração.

Essa separação permite que cada equipe teste sua parte com dados reais ou simulados. A integração completa só precisa ser feita depois que os contratos estiverem estáveis.
