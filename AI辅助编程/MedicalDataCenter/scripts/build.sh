#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SKIP_DOCKER_BUILD="${SKIP_DOCKER_BUILD:-false}"
SKIP_FRONTEND="${SKIP_FRONTEND:-false}"
SKIP_BACKEND="${SKIP_BACKEND:-false}"

run_step() {
  echo "==> $1"
  shift
  "$@"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "未找到命令: $1" >&2
    exit 1
  fi
}

if [[ "$SKIP_FRONTEND" != "true" ]]; then
  if [[ -f "$ROOT_DIR/frontend/package.json" ]]; then
    require_command npm
    pushd "$ROOT_DIR/frontend" >/dev/null
    if [[ -f package-lock.json ]]; then
      run_step "构建前端" npm ci
    else
      run_step "安装前端依赖" npm install
    fi
    run_step "执行前端打包" npm run build
    popd >/dev/null
  else
    echo "警告: 未找到 frontend/package.json，跳过前端构建。" >&2
  fi
fi

if [[ "$SKIP_BACKEND" != "true" ]]; then
  if [[ -f "$ROOT_DIR/backend/pom.xml" ]]; then
    require_command mvn
    pushd "$ROOT_DIR/backend" >/dev/null
    run_step "构建后端" mvn clean package -DskipTests
    popd >/dev/null
  else
    echo "警告: 未找到 backend/pom.xml，跳过后端构建。" >&2
  fi
fi

if [[ "$SKIP_DOCKER_BUILD" != "true" ]]; then
  require_command docker
  run_step "构建 Docker 镜像" docker compose -f "$ROOT_DIR/docker/docker-compose.yml" build
fi
