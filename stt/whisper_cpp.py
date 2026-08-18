"""Adaptador para o executável whisper-cli do projeto whisper.cpp.

O adaptador não baixa modelos nem compila o whisper.cpp. Essa separação é
intencional: o aplicativo decide como distribuir o binário e o modelo em cada
plataforma, enquanto este módulo apenas executa a inferência local.
"""

from __future__ import annotations

import re
import shutil
import subprocess
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Sequence

from .base import STTError, TranscriptionResult, TranscriptionSegment

_TIMESTAMP_RE = re.compile(
    r"^\[(?P<start>\d{2}:\d{2}:\d{2}(?:\.\d{3})?)\s+-->\s+"
    r"(?P<end>\d{2}:\d{2}:\d{2}(?:\.\d{3})?)\]\s*(?P<text>.*)$"
)


def _timestamp_to_seconds(value: str) -> float:
    hours, minutes, seconds = value.split(":")
    return int(hours) * 3600 + int(minutes) * 60 + float(seconds)


def parse_whisper_output(output: str) -> tuple[str, tuple[TranscriptionSegment, ...]]:
    """Extrai texto e segmentos do formato textual padrão do whisper.cpp.

    A função também aceita texto sem timestamps, o que facilita o uso com
    arquivos gerados por versões diferentes do `whisper-cli`.
    """

    segments: list[TranscriptionSegment] = []
    fallback_lines: list[str] = []

    for raw_line in output.splitlines():
        line = raw_line.strip()
        if not line:
            continue

        match = _TIMESTAMP_RE.match(line)
        if match:
            text = match.group("text").strip()
            if text:
                segments.append(
                    TranscriptionSegment(
                        text=text,
                        start_seconds=_timestamp_to_seconds(match.group("start")),
                        end_seconds=_timestamp_to_seconds(match.group("end")),
                    )
                )
            continue

        # Ignora logs típicos do executável quando não há arquivo .txt.
        if line.startswith(("whisper_", "system_info:", "main:")):
            continue
        fallback_lines.append(line)

    if segments:
        text = " ".join(segment.text for segment in segments)
    else:
        text = " ".join(fallback_lines)

    return text.strip(), tuple(segments)


@dataclass(slots=True)
class WhisperCppSTT:
    """Transcreve áudio localmente usando `whisper-cli`."""

    model_path: str | Path
    executable: str = "whisper-cli"
    default_language: str = "pt"
    timeout_seconds: float = 120.0
    extra_args: Sequence[str] = field(default_factory=tuple)

    def _resolve_executable(self) -> str:
        executable_path = Path(self.executable)
        if executable_path.parent != Path("."):
            if not executable_path.is_file():
                raise STTError(f"Executável do whisper.cpp não encontrado: {self.executable}")
            return str(executable_path)

        resolved = shutil.which(self.executable)
        if resolved is None:
            raise STTError(
                "Executável do whisper.cpp não encontrado no PATH. "
                "Compile o whisper.cpp ou informe `executable` com o caminho completo."
            )
        return resolved

    def transcribe(
        self,
        audio_path: str | Path,
        *,
        language: str | None = None,
    ) -> TranscriptionResult:
        audio = Path(audio_path)
        model = Path(self.model_path)
        if not audio.is_file():
            raise STTError(f"Arquivo de áudio não encontrado: {audio}")
        if not model.is_file():
            raise STTError(f"Modelo do whisper.cpp não encontrado: {model}")

        command = [
            self._resolve_executable(),
            "-m",
            str(model),
            "-f",
            str(audio),
            "-l",
            language or self.default_language,
            "-nt",
            "-otxt",
        ]
        command.extend(self.extra_args)

        with tempfile.TemporaryDirectory(prefix="tetirua-whisper-") as temp_dir:
            output_base = Path(temp_dir) / "transcription"
            command.extend(["-of", str(output_base)])
            try:
                completed = subprocess.run(
                    command,
                    check=False,
                    capture_output=True,
                    text=True,
                    timeout=self.timeout_seconds,
                )
            except subprocess.TimeoutExpired as exc:
                raise STTError(
                    f"A transcrição excedeu o limite de {self.timeout_seconds:.0f} segundos."
                ) from exc
            except OSError as exc:
                raise STTError(f"Não foi possível executar o whisper.cpp: {exc}") from exc

            if completed.returncode != 0:
                details = (completed.stderr or completed.stdout).strip()
                raise STTError(
                    f"whisper.cpp terminou com código {completed.returncode}: {details[-1000:]}"
                )

            generated_txt = output_base.with_suffix(".txt")
            output = generated_txt.read_text(encoding="utf-8") if generated_txt.exists() else completed.stdout

        text, segments = parse_whisper_output(output)
        return TranscriptionResult(
            text=text,
            language=language or self.default_language,
            is_final=True,
            segments=segments,
            metadata={"engine": "whisper.cpp", "model": str(model)},
        )
