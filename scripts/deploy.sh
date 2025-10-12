#!/bin/bash
# Файл: deploy.sh - деплой с автоматической проброской портов

set -e  # Остановиться при первой ошибке

echo "🚀 Начинаем деплой..."

# Сборка проекта
echo "📦 Сборка проекта..."
./gradlew clean build

# Очистка сервера
echo "🧹 Очистка сервера..."
./scripts/clean_server.sh

# Остановка всех Java процессов на сервере
echo "⏹️  Остановка Java процессов..."
ssh ifmo "pkill -9 -f java || true"

# Очистка логов и временных файлов
echo "🗑️  Очистка логов и временных файлов..."
ssh ifmo "rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/log/*"
ssh ifmo "rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/tmp/*"

# Деплой WAR файла
echo "📤 Деплой WAR файла..."
scp build/libs/is-lab1.war ifmo:~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/deployments

echo "✅ Деплой завершен!"
echo ""
echo "🌐 Для подключения к серверу с проброской портов используйте:"
echo "   ssh -L 8080:localhost:8080 -L 28600:localhost:28600 -L 28603:localhost:28603 ifmo"
echo ""
echo "📊 После подключения будут доступны:"
echo "   • Приложение:     http://localhost:8080/is-lab1"
echo "   • WildFly Admin:   http://localhost:28600"
echo "   • Management:     http://localhost:28603"
echo ""
echo "🚀 Для запуска сервера выполните на удаленной машине:"
echo '   export JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"'
echo "   ~/Web/lab3/wildfly-preview-26.1.3.Final/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0"
