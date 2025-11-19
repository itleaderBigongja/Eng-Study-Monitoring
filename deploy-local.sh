#!/bin/bash

set -e

echo "🚀 Deploying to local Kubernetes..."

# Namespace 생성
kubectl apply -f k8s-local/01-namespace.yaml

# ConfigMap 생성
kubectl apply -f k8s-local/db-init-configmap.yaml

# 인프라 배포
kubectl apply -f k8s-local/02-postgresql.yaml
kubectl apply -f k8s-local/03-elasticsearch.yaml
kubectl apply -f k8s-local/04-prometheus.yaml

echo "⏳ Waiting for infrastructure to be ready..."
sleep 30

# 애플리케이션 배포
kubectl apply -f k8s-local/05-eng-study-backend.yaml
kubectl apply -f k8s-local/06-eng-study-frontend.yaml
kubectl apply -f k8s-local/07-monitoring-backend.yaml
kubectl apply -f k8s-local/08-monitoring-frontend.yaml

echo "⏳ Waiting for applications to be ready..."
sleep 20

# Nginx 배포
kubectl apply -f k8s-local/09-nginx.yaml

echo "✅ Deployment complete!"
echo ""
echo "📊 Pod status:"
kubectl get pods -n eng-study

echo ""
echo "🌐 Access your application:"
echo "   http://localhost:30080"