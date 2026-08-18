"""monorepo principal do stt"""

from .base import (
    STTError,
    SpeechToText,
    StreamingSpeechToText,
    TranscriptionResult,
    TranscriptionSegment,
)
from .moonshine import MoonshineMicSTT
from .whisper_cpp import WhisperCppSTT, parse_whisper_output

__all__ = [
    "MoonshineMicSTT",
    "STTError",
    "SpeechToText",
    "StreamingSpeechToText",
    "TranscriptionResult",
    "TranscriptionSegment",
    "WhisperCppSTT",
    "parse_whisper_output",
]
