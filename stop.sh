#!/usr/bin/env bash

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================="
echo "🛑 Stopping Tripzy Backend..."
echo "=========================================="

# 1. Stop local process on port 8080 if running
PORT_PID=$(lsof -ti :8080 || true)
if [ -n "$PORT_PID" ]; then
  echo "Stopping local process on port 8080 (PID: $PORT_PID)..."
  kill -9 $PORT_PID 2>/dev/null || true
  echo "✅ Local process stopped."
else
  echo "ℹ️ No local process running on port 8080."
fi

# 2. Stop Docker containers if docker is running
if command -v docker &> /dev/null; then
  if docker compose version &> /dev/null; then
    docker compose down 2>/dev/null || true
  elif command -v docker-compose &> /dev/null; then
    docker-compose down 2>/dev/null || true
  fi
  echo "✅ Docker containers stopped."
fi

echo "=========================================="
echo "🎉 Everything stopped successfully."
echo "=========================================="
