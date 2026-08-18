"""monorepo principal do tts"""

from .base import SynthesisResult, TTSError, TextToSpeech
from .kokoro import KokoroTTS

__all__ = [
    "KokoroTTS",
    "SynthesisResult",
    "TTSError",
    "TextToSpeech",
]
