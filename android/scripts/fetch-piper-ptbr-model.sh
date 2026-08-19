#!/usr/bin/env bash
set -euo pipefail

ASSET="vits-piper-pt_BR-faber-medium-int8.tar.bz2"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/${ASSET}"
EXPECTED_SHA256="dbc8b1d7d729fd417ea78a350ed35696c928770ac93513d3f507bd4e88eee3fd"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/android/local-models/piper"
MODEL_DIR="${OUTPUT_DIR}/vits-piper-pt_BR-faber-medium-int8"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

command -v curl >/dev/null || { echo "curl é necessário" >&2; exit 1; }
command -v sha256sum >/dev/null || { echo "sha256sum é necessário" >&2; exit 1; }
command -v tar >/dev/null || { echo "tar é necessário" >&2; exit 1; }

mkdir -p "${OUTPUT_DIR}"
curl --fail --location --silent --show-error "${URL}" -o "${TMP_DIR}/${ASSET}"
printf '%s  %s\n' "${EXPECTED_SHA256}" "${TMP_DIR}/${ASSET}" | sha256sum --check --status

rm -rf "${MODEL_DIR}"
tar -xjf "${TMP_DIR}/${ASSET}" -C "${OUTPUT_DIR}"
printf 'Modelo Piper pt_BR instalado em %s\n' "${MODEL_DIR}"
