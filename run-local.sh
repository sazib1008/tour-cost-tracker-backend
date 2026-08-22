#!/usr/bin/env bash

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================="
echo "🚀 Starting Tripzy Backend Locally..."
echo "=========================================="

# Load environment variables from .env if present
if [ -f .env ]; then
  echo "🔑 Loading environment variables from .env..."
  set -a
  source .env
  set +a
fi

# Check if port 8080 is already in use
PORT_PID=$(lsof -ti :8080 || true)
if [ -n "$PORT_PID" ]; then
  echo "⚠️ Port 8080 is currently in use by PID $PORT_PID. Stopping previous process..."
  kill -9 $PORT_PID 2>/dev/null || true
  sleep 1
fi

# Ensure gradlew has executable permissions
chmod +x ./gradlew

echo "📦 Running ./gradlew bootRun..."
./gradlew bootRun
