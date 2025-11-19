#!/bin/bash

set -e  # 에러 발생 시 중단

echo "🔨 Building Docker images for local Kubernetes..."

# eng-study 백엔드
echo "📦 Building eng-study..."
cd eng-study
docker build -t eng-study:local .
cd ..

# eng-study 프론트엔드
echo "📦 Building eng-study-frontend..."
cd eng-study-frontend
docker build -t eng-study-frontend:local .
cd ..

# study-monitoring 백엔드
echo "📦 Building study-monitoring..."
cd study-monitoring
docker build -t study-monitoring:local .
cd ..

# study-monitoring 프론트엔드
echo "📦 Building study-monitoring-frontend..."
cd study-monitoring-frontend
docker build -t study-monitoring-frontend:local .
cd ..

echo "✅ All images built successfully!"
echo ""
echo "📋 Built images:"
docker images | grep -E "eng-study|study-monitoring"