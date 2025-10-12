#!/bin/bash
# Файл: start_server.sh - запуск WildFly сервера на удаленной машине

echo "🚀 Запуск WildFly сервера..."

# Переход в директорию WildFly
cd ~/Web/lab3/wildfly-preview-26.1.3.Final

echo "📋 Настройка Java опций..."
export JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"

echo "🌐 Запуск сервера на всех интерфейсах..."
echo "   HTTP:        http://localhost:8080"
echo "   Admin:       http://localhost:28600"
echo "   Management:  http://localhost:28603"
echo ""
echo "⏹️  Для остановки используйте Ctrl+C"
echo ""

# Запуск сервера
./bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
