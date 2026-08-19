#!/usr/bin/env bash
set -euo pipefail

MODEL_NAME="sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8"
ARCHIVE="${MODEL_NAME}.tar.bz2"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/${ARCHIVE}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/android/local-models/parakeet"
MODEL_DIR="${OUTPUT_DIR}/${MODEL_NAME}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

command -v curl >/dev/null || { echo "curl é necessário" >&2; exit 1; }
command -v tar >/dev/null || { echo "tar é necessário" >&2; exit 1; }

mkdir -p "${OUTPUT_DIR}"
if [[ ! -f "${TMP_DIR}/${ARCHIVE}" ]]; then
  curl --fail --location --silent --show-error --progress-bar "${URL}" -o "${TMP_DIR}/${ARCHIVE}"
fi

rm -rf "${MODEL_DIR}"
tar -xjf "${TMP_DIR}/${ARCHIVE}" -C "${OUTPUT_DIR}"

test -f "${MODEL_DIR}/encoder.int8.onnx"
test -f "${MODEL_DIR}/decoder.int8.onnx"
test -f "${MODEL_DIR}/joiner.int8.onnx"
test -f "${MODEL_DIR}/tokens.txt"

printf 'Modelo Parakeet TDT v3 instalado em: %s\n' "${MODEL_DIR}"
du -sh "${MODEL_DIR}"
