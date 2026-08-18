package br.com.tetirua.audio.stt

import br.com.tetirua.audio.core.NativeSttRuntime
import br.com.tetirua.audio.core.PortBackedSttEngine
import br.com.tetirua.audio.core.SpeechToTextEngine

/** Adaptador de contrato para whisper.cpp ou TFLite tiny/base multilíngue. */
class WhisperCppTfliteSttEngine(
    runtime: NativeSttRuntime,
) : SpeechToTextEngine by PortBackedSttEngine(runtime)
