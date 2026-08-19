#!/usr/bin/env bash
set -euo pipefail

ASSET="kokoro-int8-multi-lang-v1_1.tar.bz2"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/${ASSET}"
EXPECTED_SHA256="a1e94694776049035c4f2c6529f003aaece993c76aae9a78995831c3c4dcafc6"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/android/local-models/kokoro"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

command -v curl >/dev/null || { echo "curl é necessário" >&2; exit 1; }
command -v sha256sum >/dev/null || { echo "sha256sum é necessário" >&2; exit 1; }
command -v tar >/dev/null || { echo "tar é necessário" >&2; exit 1; }

mkdir -p "${OUTPUT_DIR}"
curl --fail --location --silent --show-error "${URL}" -o "${TMP_DIR}/${ASSET}"
printf '%s  %s\n' "${EXPECTED_SHA256}" "${TMP_DIR}/${ASSET}" | sha256sum --check --status

rm -rf "${OUTPUT_DIR}/kokoro-int8-multi-lang-v1_1"
tar -xjf "${TMP_DIR}/${ASSET}" -C "${OUTPUT_DIR}"
printf 'Modelo Kokoro instalado em %s\n' "${OUTPUT_DIR}/kokoro-int8-multi-lang-v1_1"
