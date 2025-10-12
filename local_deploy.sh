#!/bin/bash
# Файл: local_deploy.sh - локальный деплой и запуск WildFly

set -e  # Остановиться при первой ошибке

WILDFLY_HOME="/Users/arekalov/Yandex.Disk.localized/Itmo/5/IS/wildfly-preview-26.1.3.Final"
DEPLOYMENT_DIR="$WILDFLY_HOME/standalone/deployments"
CONFIG_DIR="$WILDFLY_HOME/standalone/configuration"

echo "🚀 Начинаем локальный деплой..."

# Проверка наличия PostgreSQL JDBC драйвера
if [ ! -f "postgresql.jar" ]; then
    echo "📥 Скачивание PostgreSQL JDBC драйвера..."
    curl -o postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.6.0.jar
fi

# Сборка проекта
echo "📦 Сборка проекта..."
./gradlew clean build

# Остановка WildFly если он запущен
echo "⏹️  Проверка и остановка WildFly..."
pkill -f wildfly || true
sleep 2

# Очистка логов и временных файлов
echo "🗑️  Очистка логов и временных файлов..."
rm -rf "$WILDFLY_HOME/standalone/log/*"
rm -rf "$WILDFLY_HOME/standalone/tmp/*"

# Копирование WAR файла
echo "📤 Копирование WAR файла..."
cp build/libs/is-lab1.war "$DEPLOYMENT_DIR"

# Настройка переменных окружения для Java
export JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.security=ALL-UNNAMED \
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens=java.management/javax.management=ALL-UNNAMED \
--add-opens=java.naming/javax.naming=ALL-UNNAMED"

echo "🚀 Запуск WildFly..."
echo ""
echo "📊 Будут доступны:"
echo "   • Приложение:     http://localhost:8080/is-lab1"
echo "   • WildFly Admin:  http://localhost:9990"
echo ""
echo "💡 Для остановки нажмите Ctrl+C"
echo ""

# Запуск WildFly
"$WILDFLY_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0