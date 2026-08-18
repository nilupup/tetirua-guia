package br.com.tetirua.audio.stt

import br.com.tetirua.audio.core.NativeSttRuntime
import br.com.tetirua.audio.core.PortBackedSttEngine
import br.com.tetirua.audio.core.SpeechToTextEngine

/** Adaptador de contrato para Moonshine tiny/base v2. */
class MoonshineSttEngine(
    runtime: NativeSttRuntime,
) : SpeechToTextEngine by PortBackedSttEngine(runtime)
