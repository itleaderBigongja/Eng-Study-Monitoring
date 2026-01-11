#!/bin/bash

# =================================================================
# ✅ [추가됨] 오늘 날짜 설정 (YYYY.MM.DD 형식)
# =================================================================
TODAY=$(date +%Y.%m.%d)
echo "📅 Today is: $TODAY"
echo "🗑️  Starting smart cleanup..."
echo ""

# =================================================================
# 1. ✅ [추가됨] Elasticsearch 스마트 정리 (과거 인덱스 삭제)
# 물리 파일을 지우기 전에 API로 '논리적 삭제'를 먼저 수행해야 합니다.
# =================================================================
echo "🔍 Checking Elasticsearch indices for cleanup..."

# 실행 중인 ES 파드 찾기
ES_POD=$(kubectl get pods -n monitoring --field-selector=status.phase=Running -l app=elasticsearch -o jsonpath="{.items[0].metadata.name}" 2>/dev/null)

if [ -n "$ES_POD" ]; then
    echo "   -> Found Running Elasticsearch pod: $ES_POD"

    # 인덱스 목록 조회 (시스템 인덱스 제외)
    INDICES=$(kubectl exec "$ES_POD" -n monitoring -- curl -s "localhost:9200/_cat/indices?h=index" | grep -v "^\.")

    if [ -z "$INDICES" ]; then
        echo "   -> ℹ️  No user indices found."
    else
        echo "   -> 🧹 Scanning for indices older than $TODAY..."

        for index in $INDICES; do
            # 정규식으로 날짜 추출 (예: logs-2026.01.11 -> 2026.01.11)
            if [[ $index =~ ([0-9]{4}\.[0-9]{2}\.[0-9]{2}) ]]; then
                INDEX_DATE="${BASH_REMATCH[1]}"

                # ✅ 날짜 비교: 인덱스 날짜가 오늘보다 작으면(과거면) 삭제
                if [[ "$INDEX_DATE" < "$TODAY" ]]; then
                    echo "      🔥 Deleting OLD index: $index (Date: $INDEX_DATE)"
                    kubectl exec "$ES_POD" -n monitoring -- curl -s -X DELETE "localhost:9200/$index" > /dev/null
                else
                    echo "      ✅ Keeping TODAY's index: $index"
                fi
            else
                echo "      ⚠️  Skipping non-dated index: $index"
            fi
        done
    fi
else
    echo "   -> ⚠️  Elasticsearch is not running. Skipping index cleanup."
fi
echo ""

# =================================================================
# 2. Deployment 및 Pod 종료 (Lock 해제)
# =================================================================
echo "🛑 Stopping Applications..."
kubectl delete deployment --all -n monitoring --ignore-not-found=true
kubectl delete deployment --all -n eng-study --ignore-not-found=true
kubectl delete statefulset --all -n monitoring --ignore-not-found=true 2>/dev/null
echo "   -> Stop signals sent."

# =================================================================
# 3. 데이터(PVC) 및 Namespace 정리
# =================================================================
echo "💾 Deleting PVCs and Namespaces..."
kubectl delete pvc --all -n monitoring --ignore-not-found=true &
kubectl delete pvc --all -n eng-study --ignore-not-found=true &
kubectl delete namespace eng-study --ignore-not-found &
kubectl delete namespace monitoring --ignore-not-found &

# =================================================================
# 4. 삭제 대기
# =================================================================
echo "⏳ Waiting for cleanup to finish..."
kubectl wait --for=delete namespace/eng-study --timeout=60s 2>/dev/null
kubectl wait --for=delete namespace/monitoring --timeout=60s 2>/dev/null
echo "   -> Namespace cleanup finished."
echo ""

# =================================================================
# 5. ⚠️ [핵심 수정] 호스트 디렉토리 정리 (물리 파일)
# =================================================================

# 로그 폴더: 삭제해도 됨 (이미 ES에 들어갔거나 필요 없음)
if [ -d "/tmp/k8s-logs" ]; then
    echo "📂 Removing host log directory (/tmp/k8s-logs)..."
    rm -rf /tmp/k8s-logs
    echo "   -> Logs deleted."
fi

# ⚠️ [중요] 데이터 폴더 삭제 부분 주석 처리!
# 위에서 1번 로직(스마트 삭제)을 통해 '오늘 데이터'를 살리기로 했으므로,
# 여기서 rm -rf를 해버리면 모든 노력이 물거품이 됩니다.
# 따라서 데이터 폴더는 지우지 않고 남겨둡니다.
if [ -d "/tmp/k8s-data" ]; then
    echo "📂 Preserving host DATA directory (keeping today's data)..."
    # rm -rf /tmp/k8s-data # 이 명령어를 주석하면 오늘 날짜는 인덱스는 살아 남는다.
    echo "   -> Data preserved."
else
    echo "📂 Host data directory not found (OK)."
fi
echo ""

# =================================================================
# 6. PV 잔재 및 Docker 정리
# =================================================================
echo "🧹 Final sweep..."
REMAINING_PVS=$(kubectl get pv | grep -E "eng-study|monitoring" | awk '{print $1}')
if [ -n "$REMAINING_PVS" ]; then
    echo "$REMAINING_PVS" | xargs kubectl delete pv --grace-period=0 --force 2>/dev/null
    echo "   -> Orphaned PVs deleted."
fi

echo "🐳 Cleaning up Docker images..."
docker rmi eng-study:local 2>/dev/null || true
docker rmi eng-study-backend:local 2>/dev/null || true
docker rmi eng-study-frontend:local 2>/dev/null || true
docker rmi study-monitoring:local 2>/dev/null || true
docker image prune -f > /dev/null

echo ""
echo "✅ Cleanup complete!"