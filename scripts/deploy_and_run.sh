#!/bin/bash
# Файл: deploy_and_run.sh - деплой + запуск + логи

set -e

# Определяем корневую директорию проекта
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🚀 Деплой + запуск + логи"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Шаг 1: Сборка проекта
echo "📦 Сборка проекта..."
cd "$PROJECT_ROOT"
./gradlew clean build

# Шаг 2: Очистка сервера
echo "🧹 Очистка сервера..."
"$SCRIPT_DIR/clean_server.sh"

# Шаг 3: Остановка Java процессов
echo "⏹️  Остановка Java процессов..."
ssh ifmo "pkill -9 -f java || true"

# Шаг 4: Очистка логов
echo "🗑️  Очистка логов и временных файлов..."
ssh ifmo "rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/log/*"
ssh ifmo "rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/tmp/*"

# Шаг 5: Деплой WAR файла
echo "📤 Деплой WAR файла..."
scp "$PROJECT_ROOT/build/libs/is-lab1.war" ifmo:~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/deployments

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Деплой завершен! Запуск сервера..."
echo ""
echo "📊 Будут доступны:"
echo "   • Приложение:     http://localhost:8080/is-lab1"
echo "   • WildFly Admin:  http://localhost:28600"
echo "   • Management:     http://localhost:28603"
echo ""
echo "⏹️  Для остановки используйте Ctrl+C"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Шаг 6: Запуск сервера с логами
ssh -L 8080:localhost:8080 \
    -L 28600:localhost:28600 \
    -L 28603:localhost:28603 \
    ifmo \
    'cd ~/Web/lab3/wildfly-preview-26.1.3.Final && \
     export JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED" && \
     ./bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0'
