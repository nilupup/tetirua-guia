package br.com.tetirua.audio.tts

import br.com.tetirua.audio.core.NativeTtsRuntime
import br.com.tetirua.audio.core.PortBackedTtsEngine
import br.com.tetirua.audio.core.TextToSpeechEngine

/** Adaptador de contrato para Kokoro-82M e suas vozes. */
class KokoroTtsEngine(
    runtime: NativeTtsRuntime,
) : TextToSpeechEngine by PortBackedTtsEngine(runtime)
