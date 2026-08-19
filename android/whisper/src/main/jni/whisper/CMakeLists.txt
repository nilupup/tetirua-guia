cmake_minimum_required(VERSION 3.22.1)

project(tetirua_whisper)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(WHISPER_CPP_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../../../third_party/whisper.cpp" CACHE PATH "Path to whisper.cpp")
if(NOT EXISTS "${WHISPER_CPP_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "whisper.cpp submodule not found at ${WHISPER_CPP_DIR}. Run git submodule update --init --recursive.")
endif()

# The pinned submodule declares project version 1.9.2. The Android target
# compiles whisper.cpp directly, so provide the same definition as upstream.
set(WHISPER_VERSION "1.9.2")
message(STATUS "Tetirua whisper.cpp version: ${WHISPER_VERSION}")

set(GGML_HOME "${WHISPER_CPP_DIR}/ggml" CACHE PATH "Path to ggml")
include(FetchContent)
FetchContent_Declare(
    ggml
    SOURCE_DIR "${GGML_HOME}"
)
FetchContent_MakeAvailable(ggml)

set(WHISPER_16K_LINK_OPTIONS
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

set(WHISPER_INCLUDE_DIRS
    "${WHISPER_CPP_DIR}"
    "${WHISPER_CPP_DIR}/src"
    "${WHISPER_CPP_DIR}/include"
    "${WHISPER_CPP_DIR}/ggml/include"
    "${WHISPER_CPP_DIR}/ggml/src"
    "${WHISPER_CPP_DIR}/ggml/src/ggml-cpu"
)

add_library(whisper SHARED
    "${WHISPER_CPP_DIR}/src/whisper.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/jni.c"
)

target_include_directories(whisper PRIVATE ${WHISPER_INCLUDE_DIRS})
target_compile_definitions(
    whisper
    PRIVATE
    GGML_USE_CPU
    WHISPER_VERSION="${WHISPER_VERSION}"
)
target_compile_options(whisper PRIVATE -O3 -fvisibility=hidden -fvisibility-inlines-hidden)
target_link_options(whisper PRIVATE
    --gc-sections
    --exclude-libs,ALL
    ${WHISPER_16K_LINK_OPTIONS}
)
target_link_libraries(whisper log android ggml)

# Android 16 devices can use 16 KB memory pages. Apply the same linker
# options to every GGML shared target that is packaged with libwhisper.so.
foreach(ggml_target IN ITEMS ggml ggml-base ggml-cpu ggml-blas ggml-vulkan)
    if(TARGET ${ggml_target})
        target_link_options(${ggml_target} PRIVATE ${WHISPER_16K_LINK_OPTIONS})
    endif()
endforeach()
