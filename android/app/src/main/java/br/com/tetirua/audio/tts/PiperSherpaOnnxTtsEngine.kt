package br.com.tetirua.audio.tts

import br.com.tetirua.audio.core.NativeTtsRuntime
import br.com.tetirua.audio.core.PortBackedTtsEngine
import br.com.tetirua.audio.core.TextToSpeechEngine

/** Adaptador de contrato para vozes Piper executadas pelo sherpa-onnx. */
class PiperSherpaOnnxTtsEngine(
    runtime: NativeTtsRuntime,
) : TextToSpeechEngine by PortBackedTtsEngine(runtime)
