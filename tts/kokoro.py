"""Adaptador local para Kokoro-82M.

As dependências e os pesos são carregados somente quando `synthesize` é
chamado. Assim, importar o pacote Tetiruã não exige PyTorch, espeak-ng ou o
download dos modelos.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from .base import SynthesisResult, TTSError


@dataclass(slots=True)
class KokoroTTS:
    """Sintetiza texto com uma voz Kokoro configurada."""

    lang_code: str = "p"
    voice: str = "pf_dora"
    speed: float = 1.0
    sample_rate: int = 24_000
    split_pattern: str = r"\n+"

    _pipeline: Any = field(default=None, init=False, repr=False)

    def _get_pipeline(self) -> Any:
        if self._pipeline is not None:
            return self._pipeline
        try:
            from kokoro import KPipeline
        except ImportError as exc:
            raise TTSError(
                "Kokoro não está instalado. Use `pip install -e '.[tts]'` "
                "e instale o espeak-ng no sistema."
            ) from exc

        try:
            self._pipeline = KPipeline(lang_code=self.lang_code)
        except Exception as exc:
            raise TTSError(f"Não foi possível carregar o Kokoro: {exc}") from exc
        return self._pipeline

    @staticmethod
    def _as_numpy(audio: Any) -> Any:
        """Converte tensores comuns para um array aceito pelo soundfile."""

        if hasattr(audio, "detach"):
            audio = audio.detach()
        if hasattr(audio, "cpu"):
            audio = audio.cpu()
        if hasattr(audio, "numpy"):
            audio = audio.numpy()
        return audio

    def synthesize(self, text: str, output_path: str | Path) -> SynthesisResult:
        text = text.strip()
        if not text:
            raise TTSError("O texto para síntese não pode estar vazio.")
        if not 0.25 <= self.speed <= 4.0:
            raise TTSError("`speed` deve estar entre 0.25 e 4.0.")

        destination = Path(output_path)
        destination.parent.mkdir(parents=True, exist_ok=True)

        try:
            import numpy as np
            import soundfile as sf
        except ImportError as exc:
            raise TTSError(
                "As dependências do Kokoro não estão instaladas. "
                "Use `pip install -e '.[tts]'`."
            ) from exc

        chunks: list[Any] = []
        try:
            generator = self._get_pipeline()(
                text,
                voice=self.voice,
                speed=self.speed,
                split_pattern=self.split_pattern,
            )
            for _, _, audio in generator:
                chunks.append(self._as_numpy(audio))
        except Exception as exc:
            if isinstance(exc, TTSError):
                raise
            raise TTSError(f"Falha na síntese com Kokoro: {exc}") from exc

        if not chunks:
            raise TTSError("O Kokoro não gerou nenhum segmento de áudio.")

        audio = np.concatenate(chunks) if len(chunks) > 1 else chunks[0]
        sf.write(destination, audio, self.sample_rate)
        return SynthesisResult(
            audio_path=destination,
            sample_rate=self.sample_rate,
            voice=self.voice,
            metadata={"engine": "kokoro", "language": "pt-BR"},
        )
