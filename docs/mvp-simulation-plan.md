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

## Ordem recomendada

A primeira prova funcional deve usar `feat/stt-whisper-cpp-tflite` ou `feat/stt-moonshine` para STT e `feat/tts-native` para TTS. O Android TTS reduz o risco da primeira demonstração, enquanto o STT deve ser escolhido após validar o português brasileiro.

Depois que o ciclo de voz estiver estável, a branch de TTS Kokoro pode ser usada para comparar qualidade neural no PC. A integração Piper + sherpa-onnx deve ser avaliada quando o projeto precisar executar TTS neural diretamente no celular.

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
