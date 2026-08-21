#!/usr/bin/env bash

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================="
echo "🐳 Starting Tripzy Backend with Docker..."
echo "=========================================="

if ! command -v docker &> /dev/null; then
  echo "❌ Error: Docker is not installed or not in PATH."
  exit 1
fi

if docker compose version &> /dev/null; then
  DOCKER_COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "❌ Error: Neither 'docker compose' nor 'docker-compose' found."
  exit 1
fi

echo "📦 Building and starting containers in background..."
$DOCKER_COMPOSE_CMD up --build -d

echo "✅ Container started successfully."
echo "🌐 Server running at: http://localhost:8080"
echo "📜 Attaching to logs (Press Ctrl+C to detach without stopping):"
$DOCKER_COMPOSE_CMD logs -f tripzy-backend
