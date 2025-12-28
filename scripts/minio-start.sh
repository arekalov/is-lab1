#!/bin/bash
# Запуск MinIO

source ~/Yandex.Disk.localized/Itmo/5/IS/minio-data/.minio/config
nohup minio server ~/Yandex.Disk.localized/Itmo/5/IS/minio-data --console-address ":9001" > ~/Yandex.Disk.localized/Itmo/5/IS/minio-data/minio.log 2>&1 &
echo $! > ~/Yandex.Disk.localized/Itmo/5/IS/minio-data/.minio.pid

echo "MinIO запущен"
echo "API: http://localhost:9000"
echo "Console: http://localhost:9001"
echo "Логи: ~/Yandex.Disk.localized/Itmo/5/IS/minio-data/minio.log"




