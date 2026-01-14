#!/bin/bash

set -e

echo "🚀 Deploying to local Kubernetes..."
echo ""

# 1. 호스트 로그 디렉토리 자동 생성 및 권한 설정
echo "📂 Setting up host log directory..."
if [ ! -d "/tmp/k8s-logs" ]; then
    echo "  Creating /tmp/k8s-logs..."
    mkdir -p /tmp/k8s-logs
fi
echo "  Setting permissions for /tmp/k8s-logs..."
chmod 777 /tmp/k8s-logs
echo "✅ Log directory ready"

echo ""

# 2. 이미지 존재 확인 (태그 포함)
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

# 3. Namespace 생성
echo "📦 Creating namespace..."
kubectl apply -f k8s-local/01-namespace.yaml

# 4. 인프라 배포
echo ""
echo "🏗️  Deploying infrastructure..."

echo "  📊 PostgreSQL"
kubectl apply -f k8s-local/02-postgresql.yaml

echo "  🔍 Elasticsearch + Kibana"
kubectl apply -f k8s-local/03-elasticsearch.yaml

# Logstash 배포
echo "  🦁 Logstash"
kubectl apply -f k8s-local/11-logstash.yaml

echo "  📈 Prometheus"
kubectl apply -f k8s-local/04-prometheus.yaml

# [중요] 리소스 등록 대기
echo "⏳ Waiting 5s for resources to be registered..."
sleep 5

echo ""
echo "⏳ Waiting for infrastructure to be ready..."

# 각 인프라 대기
echo "  Waiting for PostgreSQL..."
kubectl wait --for=condition=ready pod -l app=postgres -n eng-study --timeout=180s

echo "  Waiting for Elasticsearch..."
kubectl wait --for=condition=ready pod -l app=elasticsearch -n monitoring --timeout=180s

echo "  Waiting for Kibana..."
kubectl wait --for=condition=ready pod -l app=kibana -n monitoring --timeout=180s

echo "  Waiting for Logstash..."
kubectl wait --for=condition=ready pod -l app=logstash -n monitoring --timeout=180s

echo "  Waiting for Prometheus..."
kubectl wait --for=condition=ready pod -l app=prometheus -n monitoring --timeout=180s

echo ""
echo "✅ Infrastructure is ready!"

# 추가 대기 (데이터베이스 초기화 시간)
echo ""
echo "⏳ Waiting for database initialization (10s)..."
sleep 10

# 5. 애플리케이션 배포
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

# 6. Nginx 배포
echo ""
echo "🌐 Deploying Nginx..."
kubectl apply -f k8s-local/09-nginx.yaml

echo ""
echo "⏳ Waiting for Nginx to be ready (10s)..."
sleep 10

# 7. 최종 상태 확인
echo ""
echo "✅ Deployment complete!"
echo ""
echo "📊 Pod status:"
kubectl get pods -n eng-study

echo ""
echo "🔌 Services:"
kubectl get svc -n eng-study

# 8. 로그 파일 확인
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📝 Checking log files..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
sleep 5
POD_NAME=$(kubectl get pod -n eng-study -l app=eng-study-backend -o jsonpath='{.items[0].metadata.name}')
if [ -n "$POD_NAME" ]; then
    echo "✅ Pod: $POD_NAME"
    echo ""
    echo "📂 Log files in pod:"
    kubectl exec -n eng-study $POD_NAME -- ls -lh /logs/
    echo ""
    echo "📂 Log files on host:"
    ls -lh /tmp/k8s-logs/ 2>/dev/null || echo "  (empty or not accessible)"
else
    echo "❌ Pod not found"
fi

# =================================================================
# 9. Elasticsearch 연결 대기 및 인덱스 확인 (스마트 체크)
# =================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 Checking Elasticsearch Connectivity..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ES 파드 이름 찾기
ES_POD=$(kubectl get pod -n monitoring -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}')

if [ -n "$ES_POD" ]; then
    echo "   Target Pod: $ES_POD"
    echo "⏳ Waiting for Elasticsearch HTTP API to be available..."

    # 최대 30번 시도 (약 2분 대기)
    MAX_RETRIES=30
    COUNT=0
    ES_READY=0

    while [ $COUNT -lt $MAX_RETRIES ]; do
        # 파드 내부에서 curl 실행 (외부 포트 문제 회피)
        # HTTP 응답 코드만 가져옴 (-w "%{http_code}")
        HTTP_CODE=$(kubectl exec -n monitoring $ES_POD -- curl -s -o /dev/null -w "%{http_code}" "http://localhost:9200" 2>/dev/null || echo "000")

        if [ "$HTTP_CODE" == "200" ]; then
            ES_READY=1
            echo "   ✅ Elasticsearch is UP and responding! (HTTP 200)"
            break
        fi

        echo "   ... Initializing (Current status: $HTTP_CODE). Retrying in 4s... ($((COUNT+1))/$MAX_RETRIES)"
        sleep 4
        COUNT=$((COUNT+1))
    done

    echo ""
    if [ $ES_READY -eq 1 ]; then
        echo "▶ Elasticsearch Index List:"
        echo "---------------------------------------------------"
        kubectl exec -n monitoring $ES_POD -- curl -s "http://localhost:9200/_cat/indices?v"
        echo "---------------------------------------------------"
    else
        echo "⚠️ Elasticsearch did not respond with HTTP 200 within the timeout."
        echo "   (It might still be loading. Check logs with: kubectl logs -f -n monitoring $ES_POD)"
    fi
else
    echo "⚠️ Elasticsearch pod not found."
fi

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
echo "🔧 Troubleshooting:"
echo "  kubectl get events -n eng-study --sort-by='.lastTimestamp'"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"