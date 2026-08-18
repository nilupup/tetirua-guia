# Guia de revisão do Tetiruã

## Objetivo da revisão

O objetivo desta etapa é escolher uma solução de STT e uma solução de TTS para o MVP do Tetiruã. A revisão deve verificar se cada alternativa funciona localmente, se pode ser integrada ao celular e se atende ao português brasileiro, à latência esperada e ao uso conversacional do projeto.

## Onde revisar no GitHub

A branch `main` é a referência estável. As branches de solução são pontos de partida independentes:

| Branch | Pergunta principal da revisão |
|---|---|
| `feat/stt-moonshine` | Moonshine atende ao fluxo de STT local e ao idioma necessário? |
| `feat/stt-whisper-cpp-tflite` | whisper.cpp ou TFLite oferece a melhor combinação de precisão, memória e latência? |
| `feat/tts-native` | Android TTS/AVSpeechSynthesizer já resolve o áudio do MVP? |
| `feat/tts-kokoro-82m` | Kokoro oferece qualidade superior em português brasileiro no protótipo local? |
| `feat/tts-piper-sherpa-onnx` | Piper com sherpa-onnx é viável para execução neural diretamente no celular? |

No GitHub, selecione a branch no seletor de código. Em seguida, abra `README.md`, `docs/`, `stt/`, `tts/` e `tests/`. A comparação geral com a branch padrão pode ser feita em **Compare** usando a forma `main...nome-da-branch`.

## O que deve existir em uma branch pronta para análise

Uma branch de modelo deve conter uma implementação pequena e reproduzível, documentação de instalação, um teste automatizado ou um teste manual claramente descrito e um relatório com resultados. O código deve depender de uma interface comum, para que a troca de modelo não exija reescrever o orquestrador.

| Item | O que o Henrique deve encontrar |
|---|---|
| Implementação | Adaptador em `stt/` ou `tts/`, com configuração explícita de modelo e idioma. |
| Interface | Uso compatível com `TranscriptionResult` ou `SynthesisResult`. |
| Dependências | Dependências declaradas em `pyproject.toml` ou na documentação da plataforma. |
| Testes | Testes que possam rodar sem baixar modelos grandes, quando possível. |
| Benchmark | Tamanho, memória, latência, idioma, qualidade e limitações. |
| Documentação | Instruções para instalar o runtime, baixar o modelo e executar um exemplo. |
| Segurança | Nenhum token, modelo grande, áudio pessoal ou binário deve estar versionado. |

## Critérios de STT

A alternativa de STT deve ser avaliada com perguntas reais do projeto, por exemplo: “O que é esta escultura?”, “Quem construiu este prédio?” e “Qual é a história deste monumento?”. O conjunto de teste deve incluir fala natural, nomes próprios, ruído de rua e perguntas de acompanhamento.

Os resultados mínimos a registrar são a transcrição produzida, o tempo entre o fim da fala e o texto final, o consumo aproximado de memória, o tamanho do modelo, a estabilidade em execução local e a qualidade em português brasileiro. A branch não deve ser aprovada apenas porque inicia corretamente.

## Critérios de TTS

A alternativa de TTS deve ser avaliada com respostas curtas e médias semelhantes às que o LLM produzirá. Devem ser observadas clareza, pronúncia de nomes históricos e locais, naturalidade, velocidade de síntese, tempo até o início da reprodução, tamanho do áudio e facilidade de execução no celular.

Para o MVP, Android TTS pode ser aprovado como fallback mesmo que uma voz neural seja mais natural. O objetivo inicial é comprovar que o usuário recebe uma resposta falada de maneira confiável.

## Critérios para comparar as alternativas

A combinação de STT e TTS usada em uma primeira simulação é apenas uma configuração de teste. Ela não elimina as outras branches e não define sozinha o MVP do produto completo:

```text
microfone -> STT -> texto da pergunta -> orquestrador -> resposta textual -> TTS -> áudio
```

Todas as alternativas devem ser comparadas por funcionamento no dispositivo-alvo, suporte ao português brasileiro, latência, consumo de recursos, qualidade, privacidade, disponibilidade offline e facilidade de manutenção. Depois dos testes, a equipe do produto poderá escolher uma configuração para uma demonstração, uma versão do MVP ou uma etapa posterior.

## O que não deve ser feito

Não se deve comparar modelos misturando alterações de várias branches. Não se deve enviar modelos grandes, arquivos de áudio pessoais, binários compilados ou chaves de API ao GitHub. Também não se deve fazer commit diretamente na `main`; cada experimento deve ocorrer em sua própria branch e ser incorporado somente depois de uma revisão.

## Estado atual desta etapa

As cinco branches remotas foram criadas a partir da `main`, mas ainda não possuem implementação específica de modelo. A base inicial de contratos e adaptadores está na cópia local da branch `feat/audio-initial-adapters`. A próxima ação de desenvolvimento será trabalhar em cada uma das cinco branches, levando para ela somente o código correspondente ao modelo e registrando os resultados da avaliação. Nenhuma alternativa será descartada antes da comparação.

## Fluxo recomendado para o colaborador

```bash
git switch main
git pull --ff-only
git switch feat/stt-whisper-cpp-tflite
# desenvolver e testar somente o bloco escolhido
git status
git diff main...feat/stt-whisper-cpp-tflite
git add stt tests docs pyproject.toml
git commit -m "feat: evaluate whisper cpp stt"
git push -u origin feat/stt-whisper-cpp-tflite
```

Depois do push, o Henrique deve abrir a branch no GitHub, ler a documentação, verificar os testes e comparar os arquivos alterados com a `main`. Quando a equipe selecionar uma alternativa, será aberto um pull request para discutir e eventualmente fazer o merge.
