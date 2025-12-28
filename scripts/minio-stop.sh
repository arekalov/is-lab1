#!/bin/bash
# Остановка MinIO

pkill -f "minio server" && echo "MinIO остановлен" || echo "MinIO не запущен"



