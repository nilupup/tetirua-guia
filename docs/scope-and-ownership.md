# Escopo e responsabilidades da camada de áudio

## Objetivo da nossa equipe

A responsabilidade desta parte do projeto é construir uma camada completa de **Speech-to-Text (STT)** e **Text-to-Speech (TTS)** para o Tetiruã. O trabalho não consiste em escolher agora a alternativa de menor custo nem em declarar um único modelo vencedor. O objetivo é disponibilizar implementações comparáveis, documentadas e substituíveis para que a equipe possa testá-las no futuro em diferentes dispositivos e cenários.

O MVP do sistema completo é apenas uma possibilidade de integração. Ele poderá usar uma combinação simples, mas essa decisão pertence ao projeto como um todo e deve ser tomada depois dos testes de cada alternativa. A camada de áudio deve permanecer neutra em relação a essa decisão.

## Responsabilidades por equipe

| Equipe ou camada | Responsabilidade | Interface esperada para integração |
|---|---|---|
| **STT/TTS** | Capturar/transcrever voz, sintetizar respostas e comparar motores. | Texto de entrada/saída, metadados, eventos parciais e arquivos/streams de áudio. |
| **LLM** | Interpretar a pergunta, organizar o contexto e gerar a resposta textual. | Recebe texto da pergunta e contexto; devolve resposta textual e metadados. |
| **VLM** | Interpretar a imagem observada pelos óculos. | Recebe imagem; devolve descrição estruturada ou textual da cena. |
| **GPS/localização** | Fornecer posição, cidade, rua e contexto geográfico. | Coordenadas e metadados de localização. |
| **Web search** | Recuperar fontes históricas, culturais e contextuais. | Trechos, URLs, títulos e referências. |
| **Aplicação/celular** | Orquestrar captura, comunicação, exibição e reprodução. | Liga os adaptadores e controla ciclo de interação. |

A camada de áudio não deve conter lógica de LLM, VLM, localização ou web search. Isso permite testar cada parte isoladamente e trocar o motor de voz sem reescrever a aplicação principal.

## Alternativas que devem permanecer disponíveis

| Área | Alternativas | Situação esperada |
|---|---|---|
| STT | Moonshine `tiny/base v2` | Adaptador e branch próprios para testes locais e streaming. |
| STT | whisper.cpp / TFLite `tiny/base` | Adaptador e branch próprios para comparação de runtime e modelo. |
| TTS | Android TTS / AVSpeechSynthesizer | Integração nativa por plataforma, usada como referência de disponibilidade. |
| TTS | Kokoro-82M | Adaptador neural local, incluindo vozes de português brasileiro. |
| TTS | Piper + sherpa-onnx | Adaptador para execução local baseada em ONNX e futura integração móvel. |

As variantes `tiny` e `base` não são decisões finais. Elas são configurações de benchmark dentro da alternativa correspondente. O mesmo vale para Android e iOS dentro da solução de TTS nativo.

## Organização no GitHub

As cinco branches de solução correspondem aos cinco blocos da proposta:

```text
main
├── feat/stt-moonshine
├── feat/stt-whisper-cpp-tflite
├── feat/tts-native
├── feat/tts-kokoro-82m
└── feat/tts-piper-sherpa-onnx
```

Cada branch deve conter o código específico daquela alternativa, seus testes, sua documentação de instalação e seus resultados de benchmark. Nenhuma branch deve ser removida apenas porque não foi escolhida para a primeira demonstração; ela pode ser útil para comparação futura, outro dispositivo ou uma versão offline.

## Critérios de comparação

Cada alternativa deve ser analisada de forma independente, registrando idioma, plataforma, modelo, runtime, tamanho dos arquivos, memória, latência, consumo aproximado, qualidade percebida, estabilidade e limitações. O resultado do benchmark serve para decisão técnica futura; não existe uma classificação fixa de “MVP” e “posterior” antes dos testes.

## Integração futura com o restante do Tetiruã

O fluxo de integração será:

```text
STT -> pergunta textual -> LLM/VLM/GPS/web search -> resposta textual -> TTS
```

Os módulos de áudio devem aceitar dados produzidos por outras equipes sem exigir que elas conheçam a implementação interna do modelo. Em sentido inverso, os adaptadores devem devolver resultados normalizados para que o celular ou o orquestrador possa escolher o próximo passo.

## Regra de decisão

A escolha de uma combinação para uma demonstração ou MVP será feita somente após a execução dos testes. Custo é um critério possível, mas não é o único: idioma, privacidade, qualidade, latência, memória, bateria, disponibilidade offline e facilidade de integração também devem ser considerados.
