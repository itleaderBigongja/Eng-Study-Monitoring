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

# 인프라 배포
echo ""
echo "🏗️  Deploying infrastructure..."

echo "  📊 PostgreSQL"
kubectl apply -f k8s-local/02-postgresql.yaml

echo "  🔍 Elasticsearch + Kibana"
kubectl apply -f k8s-local/03-elasticsearch.yaml

echo "  📈 Prometheus"
kubectl apply -f k8s-local/04-prometheus.yaml

echo ""
echo "⏳ Waiting for infrastructure to be ready..."

# PostgreSQL 대기
echo "  Waiting for PostgreSQL..."
kubectl wait --for=condition=ready pod -l app=postgres -n eng-study --timeout=120s

# Elasticsearch 대기
echo "  Waiting for Elasticsearch..."
kubectl wait --for=condition=ready pod -l app=elasticsearch -n monitoring --timeout=120s

# Kibana 대기
echo "  Waiting for Kibana..."
kubectl wait --for=condition=ready pod -l app=kibana -n monitoring --timeout=180s

# Prometheus 대기
echo "  Waiting for Prometheus..."
kubectl wait --for=condition=ready pod -l app=prometheus -n monitoring --timeout=120s

echo ""
echo "✅ Infrastructure is ready!"

# 인프라 상태 확인
echo ""
echo "📊 Infrastructure status (eng-study):"
kubectl get pods -n eng-study | grep "postgres"

echo "📊 Infrastructure status (monitoring):"
kubectl get pods -n monitoring | grep -E "elasticsearch|kibana|prometheus"

# 추가 대기 (데이터베이스 초기화 시간)
echo ""
echo "⏳ Waiting for database initialization (10s)..."
sleep 10

# 애플리케이션 배포
echo ""
echo "📱 Deploying applications..."

echo "  🔧 eng-study backend"
kubectl apply -f k8s-local/05-eng-study-backend.yaml

echo "  🎨 eng-study frontend"
kubectl apply -f k8s-local/06-eng-study-frontend.yaml

echo "  🔧 monitoring backend"
kubectl apply -f k8s-local/07-monitoring-backend.yaml

echo "  🎨 monitoring frontend"
kubectl apply -f k8s-local/08-monitoring-frontend.yaml

echo ""
echo "⏳ Waiting for applications to be ready (30s)..."
sleep 30

# Nginx 배포
echo ""
echo "🌐 Deploying Nginx..."
kubectl apply -f k8s-local/09-nginx.yaml

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
echo "  English Study:    http://localhost:30080"
echo "  Monitoring:       http://localhost:30081"
echo "  Kibana:           http://localhost:30601"
echo "  Prometheus:       http://localhost:30100 (if exposed)"
echo ""
echo "🐘 Connect to PostgreSQL (run in new terminal):"
echo "  kubectl port-forward -n eng-study service/postgres-service 5432:5432"
echo "  Then use DBeaver: localhost:5432"
echo "    Database: DEV_DB"
echo "    Username: rnbsoft"
echo "    Password: rnbsoft"
echo ""
echo "🔍 Connect to Elasticsearch (run in new terminal):"
echo "  kubectl port-forward -n monitoring service/elasticsearch-service 9200:9200"
echo "  Then access: http://localhost:9200"
echo ""
echo "📊 View logs:"
echo "  kubectl logs -f deployment/eng-study-backend -n eng-study"
echo "  kubectl logs -f deployment/elasticsearch -n monitoring"
echo "  kubectl logs -f deployment/kibana -n monitoring"
echo ""
echo "🔧 Troubleshooting (eng-study):"
echo "  kubectl describe pod <pod-name> -n eng-study"
echo "  kubectl get events -n eng-study --sort-by='.lastTimestamp'"
echo "🔧 Troubleshooting (monitoring):"
echo "  kubectl describe pod <pod-name> -n monitoring"
echo "  kubectl get events -n monitoring --sort-by='.lastTimestamp'"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
