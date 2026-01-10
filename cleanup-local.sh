#!/bin/bash

echo "🗑️  Cleaning up local Kubernetes environment..."
echo ""

# =================================================================
# 1. 호스트 로그 디렉토리 정리
# =================================================================
if [ -d "/tmp/k8s-logs" ]; then
    echo "📂 Removing host log directory (/tmp/k8s-logs)..."
    rm -rf /tmp/k8s-logs
    echo "   -> Deleted."
else
    echo "📂 Host log directory not found (OK)."
fi
echo ""

# =================================================================
# 2. Elasticsearch 인덱스 목록 확인 (삭제 전 리포팅)
# =================================================================
echo "🔍 Checking Elasticsearch indices before destruction..."

# 파드 이름 찾기
ES_POD=$(kubectl get pods -n monitoring --field-selector=status.phase=Running -l app=elasticsearch -o jsonpath="{.items[0].metadata.name}" 2>/dev/null)

if [ -n "$ES_POD" ]; then
    echo "   -> Found Running Elasticsearch pod: $ES_POD"

    # 인덱스 목록 조회
    INDICES=$(kubectl exec "$ES_POD" -n monitoring -- curl -s "localhost:9200/_cat/indices?h=index,docs.count,store.size&s=index")

    if [ -z "$INDICES" ]; then
        echo "   -> ℹ️  Elasticsearch is empty (No indices found)."
    else
        echo "   -> 📉 The following indices will be PERMANENTLY deleted:"
        echo "      ---------------------------------------------------"
        echo "      Index Name           | Docs Count | Size"
        echo "      ---------------------------------------------------"
        echo "$INDICES" | awk '{printf "      %-20s | %-10s | %s\n", $1, $2, $3}'
        echo "      ---------------------------------------------------"
    fi
else
    echo "   -> ⚠️  Elasticsearch is not running. Skipping index check."
fi
echo ""

# =================================================================
# 3. [핵심 수정] Deployment 먼저 삭제 (연결 끊기)
# 파드를 먼저 죽여야 PVC가 물고 있는 락(Lock)이 해제됩니다.
# =================================================================
echo "🛑 Stopping Applications first (Release locks)..."
# 모든 Deployment, StatefulSet 삭제
kubectl delete deployment --all -n monitoring --ignore-not-found=true
kubectl delete deployment --all -n eng-study --ignore-not-found=true
kubectl delete statefulset --all -n monitoring --ignore-not-found=true 2>/dev/null
echo "   -> Application stop signals sent."

# =================================================================
# 4. 데이터(PVC) 삭제 (이제 안전하게 삭제됨)
# =================================================================
echo "💾 Deleting Persistent Volume Claims (DATA)..."
# 파드가 내려가는 중이므로 바로 삭제 명령을 백그라운드로 수행
kubectl delete pvc --all -n monitoring --ignore-not-found=true &
kubectl delete pvc --all -n eng-study --ignore-not-found=true &
echo "   -> PVC Delete command sent."
echo ""

# =================================================================
# 5. Namespace 및 리소스 정리
# =================================================================
echo "🔥 Deleting namespaces..."
kubectl delete namespace eng-study --ignore-not-found &
kubectl delete namespace monitoring --ignore-not-found &

# =================================================================
# 6. 삭제 대기 (Wait)
# =================================================================
echo "⏳ Waiting for cleanup to finish..."
# 타임아웃을 설정하여 무한 대기 방지
kubectl wait --for=delete namespace/eng-study --timeout=60s 2>/dev/null || echo "   -> eng-study namespace deletion timed out (forcing continuation)"
kubectl wait --for=delete namespace/monitoring --timeout=60s 2>/dev/null || echo "   -> monitoring namespace deletion timed out (forcing continuation)"
echo "   -> Namespace cleanup finished."
echo ""

# =================================================================
# 7. PV(영구 볼륨) 잔재 강제 삭제
# =================================================================
echo "🧹 Checking for remaining PersistentVolumes..."
REMAINING_PVS=$(kubectl get pv | grep -E "eng-study|monitoring" | awk '{print $1}')

if [ -n "$REMAINING_PVS" ]; then
    echo "   -> Found orphaned PVs: $REMAINING_PVS"
    echo "$REMAINING_PVS" | xargs kubectl delete pv --grace-period=0 --force
    echo "   -> Orphaned PVs deleted."
else
    echo "   -> No orphaned PVs found (Clean)."
fi

# =================================================================
# 8. Docker 이미지 정리
# =================================================================
echo ""
echo "🐳 Cleaning up Docker images..."
docker rmi eng-study:local 2>/dev/null || true
docker rmi eng-study-backend:local 2>/dev/null || true
docker rmi eng-study-frontend:local 2>/dev/null || true
docker rmi study-monitoring:local 2>/dev/null || true

echo ""
echo "🧹 Pruning unused Docker layers..."
docker image prune -f

echo ""
echo "✅ Cleanup complete!"