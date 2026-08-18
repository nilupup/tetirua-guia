# Plano de simulação do MVP

## Objetivo

Antes de conectar os óculos, a câmera e o web search, o Tetiruã deve provar o ciclo de voz em um computador ou celular: o usuário fala, o STT transforma a fala em texto, um orquestrador simulado produz uma resposta e o TTS reproduz essa resposta em áudio.

A simulação deve validar o contrato entre os módulos, não fingir que a câmera ou o LLM já estão prontos. Por isso, os componentes de visão, localização e busca podem começar como dados simulados.

## Fases de validação

| Fase | Entrada | Componentes reais | Componentes simulados | Resultado |
|---|---|---|---|---|
| 1. STT isolado | Arquivo WAV ou microfone | Um adaptador de STT | Nenhum | Transcrição em português e métricas básicas. |
| 2. TTS isolado | Texto fixo | Um adaptador de TTS | Nenhum | Arquivo ou reprodução de áudio. |
| 3. Conversa de voz | Pergunta falada | STT + TTS | Resposta fixa | Prova de ida e volta por voz. |
| 4. Orquestrador simulado | Pergunta + descrição visual + GPS | STT + TTS | VLM, GPS, web search e LLM simulados | Resposta contextual reproduzida e exibida. |
| 5. MVP integrado | Imagem, localização e pergunta | STT + TTS + VLM + GPS + LLM/web search | Apenas dados auxiliares que ainda não estiverem conectados | Fluxo próximo do comportamento dos óculos. |

## Contrato da simulação

A camada de áudio deve receber e entregar objetos simples. Um exemplo de entrada do orquestrador é:

```json
{
  "question": "Quem criou esta escultura?",
  "vision_description": "Escultura figurativa em bronze, instalada em uma praça pública.",
  "location": {
    "latitude": -23.5505,
    "longitude": -46.6333,
    "city": "São Paulo"
  }
}
```

O orquestrador devolve uma resposta textual, que pode ser entregue ao TTS:

```json
{
  "answer": "A escultura parece ser ...",
  "sources": [
    "https://exemplo.org/fonte"
  ]
}
```

O fato de os dados de VLM, GPS e web search começarem simulados não reduz a validade do teste de STT/TTS. O objetivo dessa etapa é verificar latência, tratamento de erros, troca de motor e experiência de voz.

## Critérios de aprovação

Uma combinação de STT e TTS pode ser considerada pronta para a próxima etapa quando consegue executar pelo menos cinco perguntas consecutivas sem falhar, transcreve perguntas em português com qualidade suficiente para o orquestrador, produz resposta audível em tempo aceitável e libera corretamente o microfone e os arquivos temporários.

Também devem ser registrados o modelo utilizado, o dispositivo, o idioma, o tempo de transcrição, o tempo até o início do áudio, a memória aproximada, o tamanho dos modelos e os erros observados. Os resultados devem ficar documentados na branch da alternativa analisada.

## Ordem de validação

A ordem de execução é apenas uma forma de organizar os testes e não representa uma decisão de MVP. Cada branch deve ser testada de maneira independente:

| Ordem de teste | Branches | Foco |
|---|---|---|
| 1 | `feat/stt-moonshine` e `feat/stt-whisper-cpp-tflite` | Comparar as alternativas de STT, incluindo `tiny/base`, idioma, latência e memória. |
| 2 | `feat/tts-native` | Validar Android TTS e AVSpeechSynthesizer como integrações nativas. |
| 3 | `feat/tts-kokoro-82m` | Comparar qualidade e vozes brasileiras no ambiente disponível. |
| 4 | `feat/tts-piper-sherpa-onnx` | Avaliar execução neural local e possibilidade de integração móvel. |
| 5 | Todas as combinações aprovadas | Rodar o ciclo de conversa com as alternativas que a equipe desejar comparar. |

Uma primeira simulação pode usar qualquer combinação que esteja disponível no ambiente de teste. Essa escolha serve apenas para validar o encadeamento entre os módulos; não elimina nem prioriza definitivamente as demais alternativas.

## Fluxo final esperado

```text
Usuário fala no microfone
          |
          v
        STT
          |
          v
Pergunta textual + imagem + GPS
          |
          v
VLM/LLM + web search
          |
          v
Resposta textual com fontes
          |
          v
        TTS
          |
          v
Áudio no celular/óculos + texto na tela do celular
```

A simulação deve ser construída de maneira incremental. Primeiro provamos voz para voz; depois adicionamos contexto; por fim substituímos os componentes simulados pelos componentes reais do pipeline do Tetiruã.


## Wake word e VAD na simulação

A simulação de voz deve incluir a ativação por wake word antes do STT completo. O objetivo é validar o ciclo realista de escuta sem enviar todo o áudio ao modelo pesado:

```text
escuta leve -> wake word -> VAD -> STT -> orquestrador simulado -> TTS -> reprodução
```

A primeira versão pode usar uma wake word e um VAD simulados por comandos de teste ou arquivos de áudio anotados. Isso permite validar o contrato antes de escolher o motor definitivo de keyword spotting. O comportamento esperado é:

| Etapa | Entrada | Saída esperada |
|---|---|---|
| Escuta leve | Áudio contínuo local | Nenhum envio ao orquestrador. |
| Wake word | Trecho contendo “Tetiruã” | `wake_word.detected`. |
| Espera por fala | Janela após ativação | `vad.speech.started` ou timeout. |
| Captura | Pergunta falada | Buffer local delimitado pelo VAD. |
| Fim da fala | Silêncio suficiente | `vad.speech.stopped`. |
| STT | Buffer delimitado | `stt.final`. |
| Orquestração | Texto + contexto simulado | Resposta textual. |
| TTS | Texto falado | Áudio e `tts.completed`. |

A bateria de testes deve incluir ativação correta, áudio sem wake word, wake word seguida de silêncio, ruído de rua, duas perguntas consecutivas, interrupção durante a reprodução e cancelamento pelo usuário. Para cada caso, registrar falsos acionamentos, falhas de detecção, tempo entre wake word e fala, tempo entre fim da fala e `stt.final`, tempo até o primeiro áudio e retorno ao modo `LOW_POWER_LISTENING`.

O objetivo desta etapa não é escolher o menor custo. É verificar se a cascata mantém todas as alternativas de STT e TTS intercambiáveis e se o orquestrador consegue funcionar com qualquer combinação aprovada nas branches correspondentes.
