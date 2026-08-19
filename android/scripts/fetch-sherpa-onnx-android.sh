#!/usr/bin/env bash
set -euo pipefail

VERSION="v1.13.6"
ARCHIVE="sherpa-onnx-${VERSION}-android.tar.bz2"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/${VERSION}/${ARCHIVE}"
EXPECTED_SHA256="bb7f891b259f4faee5c55d4cc10d79a3ba2b27416e9320d2c4d044400834d75b"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/android/local-native-libs"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

command -v curl >/dev/null || { echo "curl é necessário" >&2; exit 1; }
command -v sha256sum >/dev/null || { echo "sha256sum é necessário" >&2; exit 1; }
command -v tar >/dev/null || { echo "tar é necessário" >&2; exit 1; }

mkdir -p "${OUTPUT_DIR}"
curl --fail --location --silent --show-error "${URL}" -o "${TMP_DIR}/${ARCHIVE}"
printf '%s  %s\n' "${EXPECTED_SHA256}" "${TMP_DIR}/${ARCHIVE}" | sha256sum --check --status

rm -rf "${OUTPUT_DIR}/jniLibs"
tar -xjf "${TMP_DIR}/${ARCHIVE}" -C "${TMP_DIR}"
cp -R "${TMP_DIR}/jniLibs" "${OUTPUT_DIR}/jniLibs"
printf 'Bibliotecas sherpa-onnx %s instaladas em %s\n' "${VERSION}" "${OUTPUT_DIR}/jniLibs"
find "${OUTPUT_DIR}/jniLibs" -type f -name '*.so' -print | sort
