# Estratégia de branches do Tetiruã

## Princípio de organização

As pastas `stt/` e `tts/` organizam o código por responsabilidade. As **branches** organizam experimentos e implementações independentes. Portanto, não devemos criar uma branch para cada pasta; devemos criar uma branch para cada motor ou modelo que será implementado e avaliado.

A branch `main` é a referência estável do projeto. Nenhum teste de modelo deve ser desenvolvido diretamente nela. Cada alternativa começa em uma branch própria, recebe sua implementação e seus testes, e só pode ser incorporada à `main` depois de revisão e comparação com as outras alternativas.

## Estado atual

A implementação inicial está no commit local `3968308`, na branch `feat/audio-initial-adapters`. A branch `main` local continua apontando para o commit `a6b028f`, que é o estado original do repositório remoto. O envio da branch de implementação ao GitHub ainda não foi concluído porque a autenticação usada pelo ambiente foi recusada pelo GitHub para o proprietário do repositório.

## Matriz de STT

| Branch sugerida | Referência do planejamento | Papel | Classificação | Observação |
|---|---|---|---|---|
| `feat/stt-moonshine` | Moonshine tiny/base v2 | Streaming local com baixa latência. | MVP planejado | A documentação atual consultada não lista português brasileiro; deve ser validado antes de ser o STT final do MVP. |
| `feat/stt-whisper-cpp-tiny` | whisper.cpp tiny | Baseline de baixo consumo. | MVP de validação | Adequado para medir latência e memória em celular/PC; a precisão em português precisa ser medida. |
| `feat/stt-whisper-cpp-base` | whisper.cpp base | Candidato de maior qualidade que tiny. | MVP recomendado para comparação | O modelo multilíngue é o caminho mais seguro para validar perguntas em português brasileiro. |
| `feat/stt-whisper-tflite-tiny` | whisper.cpp/TFLite tiny | Variante voltada a runtime móvel TFLite. | Etapa posterior | Requer confirmar o modelo, o runtime e a integração Android antes de codificar. |
| `feat/stt-whisper-tflite-base` | whisper.cpp/TFLite base | Variante móvel com maior capacidade. | Etapa posterior | Deve ser comparada com whisper.cpp nativo e sherpa-onnx em tamanho, latência e precisão. |

A escolha prática para o primeiro benchmark deve comparar `feat/stt-moonshine`, `feat/stt-whisper-cpp-tiny` e `feat/stt-whisper-cpp-base`. A decisão não deve ser feita apenas pelo tamanho do modelo: é necessário medir transcrição em português, latência, memória, consumo de bateria e comportamento com ruído do microfone dos óculos.

## Matriz de TTS

| Branch sugerida | Referência do planejamento | Papel | Classificação | Observação |
|---|---|---|---|---|
| `feat/tts-android-tts` | Android TTS | Fallback simples e integrado ao celular Android. | MVP | É o caminho mais rápido para obter resposta falada no aparelho. |
| `feat/tts-avspeech-synthesizer` | AVSpeechSynthesizer | Fallback equivalente para dispositivos Apple. | Etapa posterior | Só é necessário se o MVP também precisar suportar iOS. |
| `feat/tts-kokoro-82m` | Kokoro-82M | Voz neural local com suporte documentado a português brasileiro. | MVP de protótipo / comparação | A biblioteca Python lista `lang_code='p'` e vozes brasileiras; a integração Android exige uma etapa própria. |
| `feat/tts-piper-sherpa-onnx` | Piper + sherpa-onnx | TTS local empacotável para Android e outras plataformas. | MVP avançado / etapa posterior | Deve ser priorizado quando a resposta neural precisar rodar diretamente no celular. |

Para o MVP funcional, `feat/tts-android-tts` deve ser a primeira implementação, porque reduz o risco de integração. Em paralelo, `feat/tts-kokoro-82m` serve para validar qualidade da voz em português no PC. A branch `feat/tts-piper-sherpa-onnx` será o caminho de evolução para uma voz neural local no Android.

## Relação com o MVP do planejamento

O PDF define como MVP original o conjunto **Moonshine + Android TTS**, com Qwen3-VL no PC e DeepSeek V4-Flash na API. A análise das documentações adiciona uma restrição importante: a lista atual de modelos do Moonshine não inclui português brasileiro. Por isso, o MVP deve manter Moonshine como branch obrigatória de validação, mas incluir `whisper.cpp base` como candidato de segurança para o idioma do usuário.

A separação recomendada fica assim:

| Grupo | STT | TTS | Objetivo |
|---|---|---|---|
| MVP planejado | Moonshine | Android TTS | Reproduzir a arquitetura descrita no PDF. |
| MVP de segurança linguística | whisper.cpp base | Android TTS | Validar português brasileiro com menor risco de cobertura de idioma. |
| Protótipo de qualidade | whisper.cpp base ou tiny | Kokoro-82M | Avaliar qualidade local de voz e transcrição fora do celular. |
| Evolução móvel | sherpa-onnx com modelo ASR/TTS compatível | Piper via sherpa-onnx ou Kokoro convertido | Reduzir dependência de serviços externos e aproximar a execução final no Android. |
| Compatibilidade futura | TFLite e AVSpeechSynthesizer | AVSpeechSynthesizer | Atender outras plataformas ou runtimes. |

## Fluxo de trabalho

Cada branch deve conter uma única alternativa de implementação, seus testes, um pequeno relatório de benchmark e a documentação de instalação. A branch não deve incluir modelos grandes, arquivos de áudio pessoais, binários ou tokens.

O fluxo sugerido é:

```bash
git switch main
git pull --ff-only
git switch -c feat/stt-whisper-cpp-base
# implementar e testar somente essa alternativa
git add stt tests docs
git commit -m "feat: add whisper cpp base stt"
git push -u origin feat/stt-whisper-cpp-base
```

Depois, a branch pode ser revisada e comparada com as demais. A incorporação na `main` deve ocorrer por pull request, nunca por trabalho direto na branch principal. Enquanto o acesso de publicação não estiver corrigido, as branches podem ser criadas e testadas localmente sem serem enviadas ao GitHub.

## Referências

1. Planejamento do Tetiruã — diagrama e PDF fornecidos no contexto do projeto.
2. [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
3. [Moonshine Voice](https://github.com/moonshine-ai/moonshine)
4. [Modelos disponíveis do Moonshine](https://moonshine-voice.readthedocs.io/en/latest/models/available-models/)
5. [Kokoro](https://github.com/hexgrad/kokoro)
6. [Vozes do Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md)
7. [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
