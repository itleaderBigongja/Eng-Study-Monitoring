#!/bin/bash

echo "🗑️  Cleaning up local Kubernetes environment..."

# Namespace 삭제 (모든 리소스 한 번에 삭제)
echo "Deleting namespaces and all resources..."
kubectl delete namespace eng-study --ignore-not-found
kubectl delete namespace monitoring --ignore-not-found

# 완전히 삭제될 때까지 대기
echo "⏳ Waiting for complete cleanup..."
kubectl wait --for=delete namespace/eng-study --timeout=120s 2>/dev/null || true
kubectl wait --for=delete namespace/monitoring --timeout=120s 2>/dev/null || true

# PersistentVolume 정리(PVC가 삭제되면 대부분 자동으로 정리되지만, 확인 차원에서)
echo "Checking for remaining PersistentVolumes..."
# 주의: PV는 클러스터 전역 리소스이므로, 만약 남아 있다면 수동으로 확인 필요합니다.
kubectl get pv | grep -E "eng-study|monitoring" || echo "No custom PVs to clean (PVs are global)"

# Elasticsearch PVC 정리
echo "Cleaning up PersistentVolumeClaims..."
# 📊 eng-study 네임스페이스의 PVC 정리 (PostgreSQL 등)
kubectl delete pvc postgres-pvc -n eng-study 2>/dev/null || echo "postgres-pvc not found in eng-study (OK)"
# 🔍 monitoring 네임스페이스의 PVC 정리 (Elasticsearch 등)
kubectl delete pvc elasticsearch-pvc -n monitoring 2>/dev/null || echo "elasticsearch-pvc not found in monitoring (OK)"

# Docker 이미지 삭제
echo ""
echo "🐳 Cleaning up Docker images..."
docker rmi eng-study:local 2>/dev/null || echo "eng-study:local not found"
docker rmi eng-study-frontend:local 2>/dev/null || echo "eng-study-frontend:local not found"
docker rmi study-monitoring:local 2>/dev/null || echo "study-monitoring:local not found"
docker rmi study-monitoring-frontend:local 2>/dev/null || echo "study-monitoring-frontend:local not found"

# 사용하지 않는 이미지 정리
echo ""
echo "🧹 Cleaning unused Docker resources..."
docker system prune -f

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "📋 Current status:"
echo "Namespaces:"
kubectl get namespace | grep -E "eng-study|monitoring" || echo "  All namespaces deleted"
echo ""
echo "Docker images:"
docker images | grep -E "eng-study|study-monitoring" || echo "  All local images removed"
