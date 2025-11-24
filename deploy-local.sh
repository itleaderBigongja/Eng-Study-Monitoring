#!/bin/bash

set -e

echo "🚀 Deploying to local Kubernetes..."
echo ""

# 이미지 존재 확인 (태그 포함)
echo "🔍 Checking Docker images..."
MISSING_IMAGES=0

if ! docker images | grep -q "eng-study.*local"; then
    echo "❌ eng-study:local image not found"
    MISSING_IMAGES=1
else
    echo "✅ eng-study:local found"
fi

if ! docker images | grep -q "eng-study-frontend.*local"; then
    echo "❌ eng-study-frontend:local image not found"
    MISSING_IMAGES=1
else
    echo "✅ eng-study-frontend:local found"
fi

if ! docker images | grep -q "study-monitoring.*local"; then
    echo "❌ study-monitoring:local image not found"
    MISSING_IMAGES=1
else
    echo "✅ study-monitoring:local found"
fi

if ! docker images | grep -q "study-monitoring-frontend.*local"; then
    echo "❌ study-monitoring-frontend:local image not found"
    MISSING_IMAGES=1
else
    echo "✅ study-monitoring-frontend:local found"
fi

if [ $MISSING_IMAGES -eq 1 ]; then
    echo ""
    echo "⚠️  Some images are missing. Please run './build-local.sh' first."
    exit 1
fi

echo "✅ All images found"
echo ""

# Namespace 생성
echo "📦 Creating namespace..."
kubectl apply -f k8s-local/01-namespace.yaml

# ConfigMap 생성
echo "📦 Creating ConfigMaps..."
kubectl apply -f k8s-local/db-init-configmap.yaml

# 인프라 배포
echo ""
echo "🏗️  Deploying infrastructure..."
kubectl apply -f k8s-local/02-postgresql.yaml
echo "  ✓ PostgreSQL"
kubectl apply -f k8s-local/03-elasticsearch.yaml
echo "  ✓ Elasticsearch"
kubectl apply -f k8s-local/04-prometheus.yaml
echo "  ✓ Prometheus"

echo ""
echo "⏳ Waiting for infrastructure to be ready (30s)..."
sleep 30

# 인프라 상태 확인
echo "Checking infrastructure status..."
kubectl get pods -n eng-study | grep -E "postgres|elasticsearch|prometheus"

# 애플리케이션 배포
echo ""
echo "📱 Deploying applications..."
kubectl apply -f k8s-local/05-eng-study-backend.yaml
echo "  ✓ eng-study backend"
kubectl apply -f k8s-local/06-eng-study-frontend.yaml
echo "  ✓ eng-study frontend"
kubectl apply -f k8s-local/07-monitoring-backend.yaml
echo "  ✓ monitoring backend"
kubectl apply -f k8s-local/08-monitoring-frontend.yaml
echo "  ✓ monitoring frontend"

echo ""
echo "⏳ Waiting for applications to be ready (20s)..."
sleep 20

# Nginx 배포
echo ""
echo "🌐 Deploying Nginx..."
kubectl apply -f k8s-local/09-nginx.yaml
echo "  ✓ Nginx"

echo ""
echo "⏳ Waiting for Nginx to be ready (10s)..."
sleep 10

# 최종 상태 확인
echo ""
echo "✅ Deployment complete!"
echo ""
echo "📊 Pod status:"
kubectl get pods -n eng-study

echo ""
echo "🔌 Services:"
kubectl get svc -n eng-study

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 Access URLs:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  English Study: http://localhost:30080"
echo "  Monitoring:    http://localhost:30080/monitoring"
echo ""
echo "🐘 Connect to PostgreSQL (run in new terminal):"
echo "  kubectl port-forward -n eng-study service/postgres-service 5432:5432"
echo "  Then use DBeaver: localhost:5432, user: eng_user, password: eng_password_123"
echo ""
echo "📊 View logs:"
echo "  kubectl logs -f deployment/eng-study-backend -n eng-study"
echo "  kubectl logs -f deployment/postgres -n eng-study"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
