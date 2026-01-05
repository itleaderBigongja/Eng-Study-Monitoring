#!/bin/bash

echo "🗑️  Cleaning up local Kubernetes environment..."
echo ""

# 1. 호스트 로그 디렉토리 정리
if [ -d "/tmp/k8s-logs" ]; then
    echo "📂 Removing host log directory (/tmp/k8s-logs)..."
    rm -rf /tmp/k8s-logs
    echo "   -> Deleted."
else
    echo "📂 Host log directory not found (OK)."
fi
echo ""

# =================================================================
# 1.5 Elasticsearch 인덱스 목록 확인 및 삭제 (강화된 버전)
# =================================================================
echo "🧹 Checking for Elasticsearch to clean indices..."

# 1. 파드 이름 찾기 (더 강력한 검색 로직)
# 'elasticsearch'가 포함된 파드 중, 'Running' 상태인 것만 찾습니다.
ES_POD=$(kubectl get pods -n monitoring --field-selector=status.phase=Running | grep "elasticsearch" | awk '{print $1}' | head -n 1)

if [ -n "$ES_POD" ]; then
    echo "   -> Found Running Elasticsearch pod: $ES_POD"

    # 2. 연결 테스트
    echo "   -> Testing connection to Elasticsearch..."
    # /dev/null 제거: 오류가 나면 화면에 보이게 함
    CONNECTION_TEST=$(kubectl exec "$ES_POD" -n monitoring -- curl -s -o /dev/null -w "%{http_code}" "localhost:9200")

    if [ "$CONNECTION_TEST" == "200" ]; then
        # 3. 인덱스 목록 조회
        INDICES=$(kubectl exec "$ES_POD" -n monitoring -- curl -s "localhost:9200/_cat/indices?h=index")

        # 공백 제거 후 인덱스 유무 확인
        CLEAN_INDICES=$(echo "$INDICES" | tr -d '[:space:]')

        if [ -z "$CLEAN_INDICES" ]; then
            echo "   -> ✅ No indices found. (Elasticsearch is already clean)"
        else
            echo "   -> 🛑 Found indices to delete:"
            echo "$INDICES" | sed 's/^/      - /'

            echo "   -> Unlocking destructive deletions (Safety Lock OFF)..."
            # [수정] 결과 출력 확인을 위해 변수에 담고 출력
            UNLOCK_RES=$(kubectl exec "$ES_POD" -n monitoring -- curl -s -X PUT "localhost:9200/_cluster/settings" \
                -H "Content-Type: application/json" \
                -d '{"transient": {"action.destructive_requires_name": false}}')
            echo "      Response: $UNLOCK_RES"

            echo "   -> Deleting all indices (*)..."
            # [수정] 결과 출력 확인을 위해 변수에 담고 출력
            DELETE_RES=$(kubectl exec "$ES_POD" -n monitoring -- curl -s -X DELETE "localhost:9200/*")
            echo "      Response: $DELETE_RES"

            echo "   -> ✅ Cleanup command executed."
        fi
    else
        echo "   -> ⚠️  Cannot connect to Elasticsearch (HTTP Code: $CONNECTION_TEST). Skipping index deletion."
    fi
else
    echo "   -> ⚠️  No running Elasticsearch pod found. Skipping index deletion."
fi
echo ""
# =================================================================

# 2. Namespace 삭제
echo "🔥 Deleting namespaces and all resources..."
kubectl delete namespace eng-study --ignore-not-found
kubectl delete namespace monitoring --ignore-not-found

# 3. 대기
echo "⏳ Waiting for complete cleanup..."
kubectl wait --for=delete namespace/eng-study --timeout=120s 2>/dev/null || true
kubectl wait --for=delete namespace/monitoring --timeout=120s 2>/dev/null || true

# 4. PV 확인
echo ""
echo "Checking for remaining PersistentVolumes..."
kubectl get pv | grep -E "eng-study|monitoring" || echo "   -> No custom PVs to clean (OK)"

# 5. Docker 이미지 삭제
echo ""
echo "🐳 Cleaning up Docker images..."
docker rmi eng-study:local 2>/dev/null || echo "   -> eng-study:local not found"
docker rmi eng-study-frontend:local 2>/dev/null || echo "   -> eng-study-frontend:local not found"
docker rmi study-monitoring:local 2>/dev/null || echo "   -> study-monitoring:local not found"
docker rmi study-monitoring-frontend:local 2>/dev/null || echo "   -> study-monitoring-frontend:local not found"

# 6. Dangling images 삭제
echo ""
echo "🧹 Cleaning unused Docker resources..."
docker image prune -f

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "📋 Current status:"
echo "Namespaces:"
kubectl get namespace | grep -E "eng-study|monitoring" || echo "  All namespaces deleted"
echo ""
echo "Docker images:"
docker images | grep -E "eng-study|study-monitoring" || echo "  All local images removed"