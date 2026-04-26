#!/bin/sh
set -e

for model in gemma4:e2b qwen2.5:7b qwen2.5:1.5b; do
  printf '{"name":"%s"}' "$model" > /tmp/payload
  echo "Pulling $model..."
  wget -qO- --post-file /tmp/payload --header 'Content-Type: application/json' http://ollama:11434/api/pull
  echo "$model done"
done
