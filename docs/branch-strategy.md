# Estratégia de branches do Tetiruã

## Princípio de organização

As pastas `stt/` e `tts/` organizam o código por responsabilidade. As **branches** organizam implementações ou experimentos independentes. Portanto, os modelos `tiny` e `base`, assim como Android e iOS, serão inicialmente tratados como variantes dentro de uma mesma branch quando pertencem ao mesmo bloco do diagrama.

A branch `main` é a referência estável do projeto. Nenhum experimento de modelo deve ser desenvolvido diretamente nela. Cada solução começa em uma branch própria, recebe sua implementação e seus testes, e só pode ser incorporada à `main` depois de revisão e comparação.

## Cinco branches principais

| Branch | Bloco do diagrama | Escopo | Classificação inicial |
|---|---|---|---|
| `feat/stt-moonshine` | Moonshine tiny/base v2 | Avaliar as variantes `tiny` e `base` do Moonshine, com foco em captura e transcrição local. | MVP planejado, condicionado à validação do português brasileiro |
| `feat/stt-whisper-cpp-tflite` | whisper.cpp / TFLite tiny/base | Comparar `tiny` e `base` e decidir entre a integração whisper.cpp e o runtime TFLite. | MVP de segurança linguística e benchmark |
| `feat/tts-native` | Android TTS / AVSpeechSynthesizer | Avaliar a síntese nativa da plataforma, começando por Android TTS e mantendo AVSpeechSynthesizer como variante iOS. | MVP funcional |
| `feat/tts-kokoro-82m` | Kokoro-82M | Avaliar vozes e geração neural local, incluindo as vozes brasileiras documentadas. | Protótipo de qualidade e possível MVP avançado |
| `feat/tts-piper-sherpa-onnx` | Piper + sherpa-onnx | Avaliar síntese local empacotável para Android, com modelos e runtime ONNX. | Etapa posterior ou MVP avançado |

Essa estrutura contém **cinco branches de solução**, exatamente correspondentes aos cinco blocos relevantes da imagem de modelos. A branch `feat/audio-initial-adapters` é uma branch de fundação técnica: contém contratos e adaptadores genéricos, mas não é uma das cinco soluções finais.

## O que será analisado dentro de cada branch

### `feat/stt-moonshine`

A branch deve testar Moonshine em fluxo de arquivo e, quando possível, em streaming pelo microfone. As variantes `tiny` e `base` ficam como configurações ou parâmetros de benchmark. Devem ser avaliados latência, consumo de memória, estabilidade do streaming e qualidade de transcrição em português brasileiro.

A documentação oficial consultada do Moonshine lista modelos para inglês, árabe, japonês, coreano, mandarim, espanhol, ucraniano e vietnamita, mas não lista português brasileiro. Assim, a branch é importante para validar a proposta original do PDF, mas não deve ser considerada automaticamente a solução final do idioma do projeto.[1]

### `feat/stt-whisper-cpp-tflite`

A branch deve comparar as variantes `tiny` e `base` e registrar qual runtime é utilizado em cada teste. O whisper.cpp será a referência principal para execução local e integração C/C++/Android; a alternativa TFLite só deve ser implementada quando o modelo e o runtime forem confirmados.

Essa branch é o candidato mais seguro para validar português brasileiro porque o Whisper possui modelos multilíngues e o whisper.cpp oferece exemplos oficiais para Android. O benchmark deve registrar tamanho do modelo, memória, latência, taxa de erro e comportamento com ruído.[2]

### `feat/tts-native`

A branch deve começar com Android TTS como uma das alternativas de referência para o celular Android. O AVSpeechSynthesizer será mantido como variante de compatibilidade para iOS. As duas implementações devem permanecer disponíveis para comparação entre plataformas, sem transformar uma delas em decisão definitiva do projeto.

O objetivo desta branch é provar o fluxo ponta a ponta: texto recebido do orquestrador, síntese local e reprodução no telefone. Qualidade de voz neural não é o foco inicial desta branch.

### `feat/tts-kokoro-82m`

A branch deve testar o Kokoro-82M no PC, inicialmente com `lang_code='p'` e as vozes brasileiras `pf_dora`, `pm_alex` e `pm_santa`. O teste deve comparar clareza, naturalidade, velocidade e tamanho do áudio produzido.[3]

O Kokoro é adequado para uma prova de qualidade em português, mas sua integração Android não deve ser presumida. Depois do benchmark no PC, será necessário decidir se o modelo será convertido ou executado por uma camada móvel compatível.

### `feat/tts-piper-sherpa-onnx`

A branch deve avaliar o conjunto Piper + sherpa-onnx como caminho de execução local no celular. O sherpa-onnx oferece APIs e exemplos para Android, Kotlin, Java e Flutter, mas a licença do framework não substitui a verificação da licença de cada voz/modelo selecionado.[4]

Essa branch é a mais próxima de uma arquitetura neural totalmente local no celular, mas possui maior complexidade de empacotamento, seleção de modelo e validação de voz em português. Por isso, não deve bloquear a primeira demonstração funcional do projeto.

## Uso em testes e integrações futuras

O PDF apresenta Moonshine + Android TTS como uma possibilidade de combinação inicial, mas isso não transforma essa combinação em decisão final. Moonshine, whisper.cpp/TFLite, Android TTS/AVSpeechSynthesizer, Kokoro-82M e Piper+sherpa-onnx devem ser implementados ou avaliados de forma independente.

| Grupo de avaliação | Alternativas | Objetivo |
|---|---|---|
| STT local | `feat/stt-moonshine` e `feat/stt-whisper-cpp-tflite` | Comparar modelos, runtimes, idiomas, latência, memória e qualidade de transcrição. |
| TTS nativo | `feat/tts-native` | Comparar as APIs nativas de Android e iOS e validar a comunicação com o celular. |
| TTS neural | `feat/tts-kokoro-82m` e `feat/tts-piper-sherpa-onnx` | Comparar qualidade, execução local, portabilidade e possibilidade de uso móvel. |

O MVP do produto completo é apenas um cenário possível de integração. Cada branch deve produzir um relatório comparável contendo, no mínimo, idioma, modelo, tamanho, memória, latência, qualidade percebida, limitações e plataforma testada. A escolha de uma combinação para uma demonstração ou release será feita depois dos testes.

## Fluxo de trabalho

O fluxo local para iniciar uma análise é:

```bash
git switch main
git pull --ff-only
git switch -c feat/stt-moonshine
# implementar e testar somente Moonshine
git add stt tests docs
git commit -m "feat: evaluate moonshine stt"
git push -u origin feat/stt-moonshine
```

Na prática, cada uma das cinco branches será trabalhada separadamente. O commit deve ser feito dentro da branch da solução, nunca diretamente na `main`. Depois do envio, a branch pode ser comparada e revisada por pull request. O merge só ocorre quando a equipe decidir que aquela implementação deve entrar em uma integração, demonstração ou release específica; as demais branches continuam disponíveis para testes futuros.

Modelos, binários, arquivos de áudio pessoais, caches e tokens não devem ser enviados ao GitHub. O `.gitignore` do projeto já contém regras para esses artefatos.

## Estado atual da cópia local

As cinco branches foram criadas localmente a partir da branch de fundação e também foram abertas no GitHub para receber implementações independentes:

- `feat/stt-moonshine`
- `feat/stt-whisper-cpp-tflite`
- `feat/tts-native`
- `feat/tts-kokoro-82m`
- `feat/tts-piper-sherpa-onnx`

A branch `main` local continua apontando para o estado original do repositório. As cinco branches remotas já foram criadas no GitHub a partir da `main`; os commits com a base local de áudio ainda aguardam publicação.

## Referências

1. [Modelos disponíveis do Moonshine](https://moonshine-voice.readthedocs.io/en/latest/models/available-models/)
2. [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
3. [Vozes do Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md)
4. [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
