"""Integração opcional do microfone do Moonshine Voice.

O catálogo oficial do Moonshine deve ser consultado antes de selecionar o
idioma. Esta implementação é um adaptador de streaming e não instala o pacote
automaticamente.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Any

from .base import STTError


PartialCallback = Callable[[str], None]
FinalCallback = Callable[[str], None]
ErrorCallback = Callable[[Exception], None]


@dataclass(slots=True)
class MoonshineMicSTT:
    """Captura e transcreve fala do microfone com callbacks."""

    language: str = "en"
    update_interval: float = 0.5
    models_directory: str | Path | None = None
    on_partial: PartialCallback | None = None
    on_final: FinalCallback | None = None
    on_error: ErrorCallback | None = None

    _mic: Any = None

    def _report_error(self, error: Exception) -> None:
        if self.on_error is not None:
            self.on_error(error)

    def _handle_partial(self, text: str) -> None:
        if self.on_partial is not None:
            self.on_partial(str(text))

    def _handle_line(self, line: Any) -> None:
        text = getattr(line, "text", line)
        if self.on_final is not None:
            self.on_final(str(text))

    def start(self) -> None:
        """Carrega o modelo, abre o microfone e começa a escutar."""

        if self._mic is not None:
            return

        try:
            from moonshine_voice import MicTranscriber
        except ImportError as exc:
            raise STTError(
                "Moonshine não está instalado. Use `pip install -e '.[stt-moonshine]'`."
            ) from exc

        mic = (
            MicTranscriber()
            .language(self.language)
            .update_interval(self.update_interval)
            .on_text(self._handle_partial)
            .on_line(self._handle_line)
            .on_error(self._report_error)
        )
        if self.models_directory is not None:
            mic.models_from(str(self.models_directory))

        try:
            mic.load()
            mic.start()
        except Exception as exc:  # a biblioteca expõe erros de áudio/modelo em runtime
            self._report_error(exc)
            raise STTError(f"Não foi possível iniciar o Moonshine: {exc}") from exc

        self._mic = mic

    def stop(self) -> None:
        """Para a captura e libera o microfone/modelo."""

        if self._mic is None:
            return
        try:
            self._mic.stop()
        finally:
            self._mic.close()
            self._mic = None
