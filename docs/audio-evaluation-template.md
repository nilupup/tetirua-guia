# Modelo de avaliação de STT/TTS

## Identificação

| Campo | Valor |
|---|---|
| Branch | `feat/...` |
| Área | STT ou TTS |
| Motor | Moonshine, whisper.cpp/TFLite, Android TTS/AVSpeechSynthesizer, Kokoro ou Piper+sherpa-onnx |
| Modelo/variante | `tiny`, `base`, voz ou modelo ONNX |
| Runtime | Python, whisper.cpp, TFLite, Android, iOS ou sherpa-onnx |
| Plataforma | PC, Android, iOS ou outro dispositivo |
| Idioma | Português brasileiro ou idioma testado |
| Data | AAAA-MM-DD |

## Métricas de execução

| Métrica | Como registrar |
|---|---|
| Tamanho do modelo | Tamanho dos arquivos necessários, sem contar caches temporários. |
| Memória | Pico aproximado durante a operação. |
| Latência | Tempo entre o fim da entrada e a disponibilidade do resultado. |
| Tempo até o primeiro resultado | Especialmente importante para streaming e reprodução de áudio. |
| Consumo | Observação qualitativa ou medição disponível no dispositivo. |
| Modo offline | Indicar se a operação funciona sem conexão. |
| Estabilidade | Número de execuções concluídas e falhas. |

## Avaliação de STT

Para STT, registrar a entrada utilizada, a transcrição produzida, a existência de texto parcial, a existência de texto final, a pontuação de erro quando houver referência anotada e os problemas observados com ruído, sotaques, nomes próprios e termos turísticos.

| Caso | Áudio de entrada | Texto esperado | Texto produzido | Observações |
|---|---|---|---|---|
| Pergunta curta | `audio_01.wav` | preencher | preencher | preencher |
| Pergunta histórica | `audio_02.wav` | preencher | preencher | preencher |
| Nome próprio/local | `audio_03.wav` | preencher | preencher | preencher |
| Ruído de rua | `audio_04.wav` | preencher | preencher | preencher |
| Pergunta de acompanhamento | `audio_05.wav` | preencher | preencher | preencher |

## Avaliação de TTS

Para TTS, registrar o texto, a voz, o formato, a taxa de amostragem, o tempo de síntese, o tempo até o início da reprodução, a clareza, a naturalidade, a pronúncia de nomes próprios e a facilidade de integração com o celular.

| Caso | Texto | Voz/modelo | Tempo de síntese | Clareza | Naturalidade | Observações |
|---|---|---|---:|---|---|---|
| Resposta curta | preencher | preencher | preencher | preencher | preencher | preencher |
| Resposta histórica | preencher | preencher | preencher | preencher | preencher | preencher |
| Nome de monumento | preencher | preencher | preencher | preencher | preencher | preencher |
| Resposta com fontes | preencher | preencher | preencher | preencher | preencher | preencher |

## Compatibilidade com o Tetiruã

| Critério | Resultado |
|---|---|
| Recebe entrada pelo contrato comum | sim/não |
| Devolve resultado normalizado | sim/não |
| Pode ser substituído sem alterar o orquestrador | sim/não |
| Suporta execução local | sim/não/parcial |
| Suporta streaming | sim/não/parcial |
| Pode ser integrado ao Android | sim/não/parcial |
| Exige serviço remoto | sim/não |
| Exige modelo ou binário fora do Git | sim/não |
| Licença verificada | sim/não/pendente |

## Conclusão da avaliação

A conclusão deve descrever **o que foi testado**, **em qual ambiente**, **quais limitações foram observadas** e **qual próximo teste é recomendado**. Ela não deve declarar que uma alternativa é “a vencedora” sem comparar os resultados com as outras branches.

Uma configuração pode ser escolhida temporariamente para uma demonstração, mas essa decisão deve ser registrada como uma escolha de integração específica. As demais alternativas permanecem válidas para outros dispositivos, versões, requisitos de privacidade ou cenários offline.
