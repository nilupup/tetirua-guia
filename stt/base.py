"""Contratos comuns para reconhecimento de fala no Tetiruã."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Mapping, Protocol, Sequence


class STTError(RuntimeError):
    """Erro esperado durante a transcrição de áudio."""


@dataclass(frozen=True, slots=True)
class TranscriptionSegment:
    """Trecho transcrito com marcação temporal opcional."""

    text: str
    start_seconds: float | None = None
    end_seconds: float | None = None


@dataclass(frozen=True, slots=True)
class TranscriptionResult:
    """Resultado normalizado de qualquer mecanismo de STT."""

    text: str
    language: str | None = None
    is_final: bool = True
    duration_seconds: float | None = None
    segments: Sequence[TranscriptionSegment] = field(default_factory=tuple)
    metadata: Mapping[str, object] = field(default_factory=dict)


class SpeechToText(Protocol):
    """Interface mínima para STT baseado em arquivo de áudio."""

    def transcribe(
        self,
        audio_path: str | Path,
        *,
        language: str | None = None,
    ) -> TranscriptionResult:
        """Transcreve um arquivo e devolve um resultado normalizado."""


class StreamingSpeechToText(Protocol):
    """Interface para mecanismos que capturam áudio do microfone continuamente."""

    def start(self) -> None:
        """Inicia a captura."""

    def stop(self) -> None:
        """Finaliza a captura e libera os recursos."""
