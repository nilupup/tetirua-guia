from pathlib import Path

import pytest

from stt import TranscriptionSegment, parse_whisper_output
from tts import KokoroTTS, TTSError


def test_parse_whisper_output_with_timestamps() -> None:
    text, segments = parse_whisper_output(
        "[00:00:01.250 --> 00:00:02.750]  Olá, Tetiruã\n"
        "[00:00:03.000 --> 00:00:04.000]  O que estou vendo?"
    )

    assert text == "Olá, Tetiruã O que estou vendo?"
    assert segments == (
        TranscriptionSegment(
            text="Olá, Tetiruã",
            start_seconds=1.25,
            end_seconds=2.75,
        ),
        TranscriptionSegment(
            text="O que estou vendo?",
            start_seconds=3.0,
            end_seconds=4.0,
        ),
    )


def test_parse_whisper_output_ignores_runtime_logs() -> None:
    text, segments = parse_whisper_output(
        "system_info: CPU\nmain: processing file\n\npergunta válida"
    )

    assert text == "pergunta válida"
    assert segments == ()


def test_kokoro_rejects_empty_text() -> None:
    with pytest.raises(TTSError, match="não pode estar vazio"):
        KokoroTTS().synthesize("  ", Path("/tmp/unused.wav"))


def test_kokoro_rejects_invalid_speed() -> None:
    with pytest.raises(TTSError, match="speed"):
        KokoroTTS(speed=0.1).synthesize("teste", Path("/tmp/unused.wav"))
