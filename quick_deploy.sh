#!/bin/bash
# Файл: quick_deploy.sh - деплой, проброс портов и запуск с логами

echo "⚡ Быстрый деплой и запуск сервера с логами..."
echo ""

# Деплой
echo "🚀 Выполняем деплой..."
./scripts/deploy.sh

echo ""
echo "✅ Деплой завершен!"
echo ""

echo "🌐 Запускаем сервер с проброской портов и показом логов..."
echo ""
echo "📊 Будут доступны:"
echo "   • Приложение:     http://localhost:8080/is-lab1"
echo "   • WildFly Admin:   http://localhost:28600"
echo "   • Management:     http://localhost:28603"
echo ""
echo "💡 Для остановки нажмите Ctrl+C"
echo ""
echo "🚀 Подключаемся и запускаем..."

# Подключение с проброской портов и запуск сервера с показом логов
ssh -L 8080:localhost:8080 -L 28600:localhost:28600 -L 28603:localhost:28603 ifmo "cd ~/Web/lab3/wildfly-preview-26.1.3.Final && export JAVA_OPTS=\"-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED\" && ./bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0"
