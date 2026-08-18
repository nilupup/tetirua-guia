# Tetiruã Guia

Repositório oficial da proposta projetada por Fernando Pupulin, Henrique Galvão e João Victor Moura.

O Tetiruã é uma aplicação de visão computacional para óculos inteligentes. O sistema observa o ambiente, identifica lugares, monumentos, esculturas, construções e outros pontos de interesse, combina a percepção visual com a localização do usuário e consulta a web para gerar informações históricas, culturais e contextuais. A interação é conversacional: o usuário pode fazer perguntas de acompanhamento, receber a resposta por áudio e visualizar o conteúdo no celular.

## Estrutura do repositório

| Diretório | Responsabilidade |
|---|---|
| `stt/` | Contratos e adaptadores de reconhecimento de fala. |
| `tts/` | Contratos e adaptadores de síntese de fala. |
| `vlm/` | Componentes futuros de visão-linguagem. |
| `llm/` | Componentes futuros de orquestração e raciocínio. |
| `tests/` | Testes unitários que não dependem de modelos pesados. |
| `docs/` | Decisões de arquitetura e documentação técnica. |

## Estado atual de STT e TTS

A primeira base de áudio usa interfaces independentes de modelo. O módulo `stt` possui um adaptador para o executável local `whisper-cli` do whisper.cpp e um adaptador opcional de microfone para Moonshine. O módulo `tts` possui um adaptador opcional para Kokoro-82M, configurado por padrão para português brasileiro.

O catálogo oficial consultado do Moonshine não lista português brasileiro entre os modelos disponíveis. Por esse motivo, Moonshine está disponível como adaptador experimental de streaming, enquanto o STT principal em português deverá ser validado com um modelo multilíngue do Whisper ou com uma alternativa compatível com sherpa-onnx.

Para a integração Android, sherpa-onnx e Android TTS permanecem como próximos caminhos de implementação. A arquitetura foi feita para permitir a substituição do motor sem alterar o orquestrador.

Consulte [`docs/audio-architecture.md`](docs/audio-architecture.md) para conhecer as decisões, referências e próximos passos. Consulte também [`docs/branch-strategy.md`](docs/branch-strategy.md) para entender a divisão por modelo, o escopo do MVP e o fluxo de branches.

## Instalação

O pacote base não instala modelos nem dependências pesadas:

```bash
python -m pip install -e .
```

Para usar Kokoro no computador:

```bash
python -m pip install -e '.[tts]'
```

Além do pacote Python, o Kokoro pode exigir `espeak-ng` instalado no sistema, conforme a documentação oficial.

Para testar o adaptador opcional do Moonshine:

```bash
python -m pip install -e '.[stt-moonshine]'
```

As dependências de desenvolvimento são instaladas com:

```bash
python -m pip install -e '.[dev]'
```

## Uso básico

### Whisper.cpp

Compile o whisper.cpp e mantenha o executável e o modelo fora do repositório. Depois, use o adaptador:

```python
from stt import WhisperCppSTT

stt = WhisperCppSTT(
    executable="/caminho/para/whisper-cli",
    model_path="/caminho/para/ggml-base.bin",
)
result = stt.transcribe("pergunta.wav")
print(result.text)
```

### Kokoro

```python
from tts import KokoroTTS

tts = KokoroTTS(voice="pf_dora")
result = tts.synthesize(
    "Este é um teste de resposta falada do Tetiruã.",
    "artifacts/resposta.wav",
)
print(result.audio_path)
```

As vozes brasileiras documentadas pelo Kokoro incluem `pf_dora`, `pm_alex` e `pm_santa`. O áudio produzido pelo adaptador é salvo em WAV com taxa de amostragem de 24 kHz.

### Moonshine em streaming

O adaptador de microfone usa callbacks para texto parcial e linha final. Ele deve ser usado somente com idiomas e modelos disponíveis na versão instalada do Moonshine:

```python
from stt import MoonshineMicSTT

stt = MoonshineMicSTT(
    language="en",
    on_partial=lambda text: print(f"Parcial: {text}"),
    on_final=lambda text: print(f"Final: {text}"),
)
try:
    stt.start()
    input("Pressione Enter para parar...\n")
finally:
    stt.stop()
```

## Testes

Os testes unitários não baixam modelos e não exigem microfone:

```bash
python -m pytest -q
```

## Versionamento

O projeto usa a branch `main`. Para trabalhar com segurança, crie uma branch específica para cada funcionalidade, faça commits pequenos e descritivos e só depois abra um pull request ou envie a alteração para a branch principal. Nunca adicione modelos grandes, binários, áudios pessoais, tokens ou chaves de API ao Git.

Exemplo de fluxo local:

```bash
git switch -c feat/audio-contracts
git status
git add stt tts tests docs pyproject.toml README.md
git commit -m "feat: add initial STT and TTS adapters"
git push -u origin feat/audio-contracts
```

## Licença

Este projeto é distribuído sob a licença MIT. As licenças dos modelos e bibliotecas de terceiros devem ser verificadas separadamente antes de qualquer distribuição do aplicativo.
