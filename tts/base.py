"""Contratos comuns para síntese de fala no Tetiruã."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Mapping, Protocol


class TTSError(RuntimeError):
    """Erro esperado durante a síntese de áudio."""


@dataclass(frozen=True, slots=True)
class SynthesisResult:
    """Metadados do áudio sintetizado."""

    audio_path: Path
    sample_rate: int | None = None
    voice: str | None = None
    format: str = "wav"
    metadata: Mapping[str, object] = field(default_factory=dict)


class TextToSpeech(Protocol):
    """Interface mínima para converter texto em arquivo de áudio."""

    def synthesize(
        self,
        text: str,
        output_path: str | Path,
    ) -> SynthesisResult:
        """Sintetiza texto e salva o áudio no caminho indicado."""
