#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ASSETS_DIR="$ROOT_DIR/android/app/src/main/assets"
LOCAL_DIR="$ROOT_DIR/android/local-models/vosk"
MODEL_NAME="vosk-model-small-pt-0.3"
MODEL_URL="https://alphacephei.com/vosk/models/${MODEL_NAME}.zip"
ZIP_PATH="$LOCAL_DIR/${MODEL_NAME}.zip"
MODEL_DIR="$ASSETS_DIR/$MODEL_NAME"

mkdir -p "$ASSETS_DIR" "$LOCAL_DIR"

if [[ ! -f "$ZIP_PATH" ]]; then
  curl -L --fail --retry 3 --progress-bar -o "$ZIP_PATH" "$MODEL_URL"
fi

rm -rf "$MODEL_DIR"
unzip -q "$ZIP_PATH" -d "$ASSETS_DIR"

test -f "$MODEL_DIR/final.mdl"
test -f "$MODEL_DIR/HCLr.fst"
test -f "$MODEL_DIR/Gr.fst"

echo "Modelo Vosk instalado em: $MODEL_DIR"
du -sh "$MODEL_DIR"
