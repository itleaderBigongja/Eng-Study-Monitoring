#!/bin/bash

echo "🗑️  Cleaning up local Kubernetes environment..."

# Namespace 삭제 (모든 리소스 한 번에 삭제)
echo "Deleting namespace and all resources..."
kubectl delete namespace eng-study --ignore-not-found

# 완전히 삭제될 때까지 대기
echo "⏳ Waiting for complete cleanup..."
kubectl wait --for=delete namespace/eng-study --timeout=120s 2>/dev/null || true

# PersistentVolume 정리 (namespace 삭제 후에도 남을 수 있음)
echo "Checking for remaining PersistentVolumes..."
kubectl get pv | grep eng-study || echo "No PVs to clean"

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
kubectl get namespace | grep eng-study || echo "  eng-study namespace deleted"
echo ""
echo "Docker images:"
docker images | grep -E "eng-study|study-monitoring" || echo "  All local images removed"
