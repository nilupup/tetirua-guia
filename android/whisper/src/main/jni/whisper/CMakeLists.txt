cmake_minimum_required(VERSION 3.22.1)

project(tetirua_whisper)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_CPP_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../../../third_party/whisper.cpp" CACHE PATH "Path to whisper.cpp")

if(NOT EXISTS "${WHISPER_CPP_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "whisper.cpp submodule not found at ${WHISPER_CPP_DIR}. Run git submodule update --init --recursive.")
endif()

# The pinned whisper.cpp submodule declares project version 1.9.2.
# The Android target compiles whisper.cpp directly, so it must provide the
# same compile definition that the upstream CMake target provides.
set(WHISPER_VERSION "1.9.2")
message(STATUS "Whisper.cpp version: ${WHISPER_VERSION}")

set(SOURCE_FILES
    "${WHISPER_CPP_DIR}/src/whisper.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/jni.c"
)

set(GGML_HOME "${WHISPER_CPP_DIR}/ggml" CACHE PATH "Path to ggml")

include(FetchContent)
FetchContent_Declare(
    ggml
    SOURCE_DIR "${GGML_HOME}"
)
FetchContent_MakeAvailable(ggml)

include_directories(
    "${WHISPER_CPP_DIR}"
    "${WHISPER_CPP_DIR}/src"
    "${WHISPER_CPP_DIR}/include"
    "${WHISPER_CPP_DIR}/ggml/include"
    "${WHISPER_CPP_DIR}/ggml/src"
    "${WHISPER_CPP_DIR}/ggml/src/ggml-cpu"
)

add_library(whisper SHARED ${SOURCE_FILES})

target_compile_definitions(
    whisper
    PRIVATE
    GGML_USE_CPU
    WHISPER_VERSION="${WHISPER_VERSION}"
)
target_compile_options(whisper PRIVATE -O3 -fvisibility=hidden -fvisibility-inlines-hidden)
target_link_options(whisper PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)

target_link_libraries(whisper log android ggml)
