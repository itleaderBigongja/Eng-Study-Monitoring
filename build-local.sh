#!/bin/bash

set -e

echo "🔨 Building Docker images with Java 21 for local Kubernetes..."
echo ""

START_TIME=$(date +%s)

# Java 21 확인
echo "☕ Java version check:"
java -version 2>&1 | head -n 1

echo ""

# eng-study 백엔드
echo "📦 [1/4] Building eng-study backend (Java 21)..."
cd eng-study
docker build -t eng-study:local .
cd ..
echo "✅ eng-study backend built"

# eng-study 프론트엔드
echo "📦 [2/4] Building eng-study frontend..."
cd eng-study-frontend
docker build -t eng-study-frontend:local .
cd ..
echo "✅ eng-study frontend built"

# study-monitoring 백엔드
echo "📦 [3/4] Building study-monitoring backend (Java 21)..."
cd study-monitoring
docker build -t study-monitoring:local .
cd ..
echo "✅ study-monitoring backend built"

# study-monitoring 프론트엔드
echo "📦 [4/4] Building study-monitoring frontend..."
cd study-monitoring-frontend
docker build -t study-monitoring-frontend:local .
cd ..
echo "✅ study-monitoring frontend built"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "✅ All images built successfully in ${DURATION}s!"
echo ""
echo "📋 Built images:"
docker images | grep -E "REPOSITORY|eng-study|study-monitoring"

echo ""
echo "🎉 Java 21 features available:"
echo "   - Virtual Threads (add spring.threads.virtual.enabled=true)"
echo "   - Pattern Matching"
echo "   - Record Patterns"
echo ""
echo "🚀 Next: Run './deploy-local.sh' to deploy to Kubernetes"