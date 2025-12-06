#!/bin/bash
# Локальный деплой и запуск WildFly
# Использование: ./deploy-local.sh

set -e  # Остановка при ошибке

# Переходим в корень проекта (на уровень выше scripts/)
cd "$(dirname "$0")/.."

# =============================================================================
# КОНФИГУРАЦИЯ
# =============================================================================
WILDFLY_HOME="/Users/arekalov/Yandex.Disk.localized/Itmo/5/IS/wildfly-preview-26.1.3.Final"
DEPLOYMENT_DIR="$WILDFLY_HOME/standalone/deployments"
APP_PORT=8080
MANAGEMENT_PORT=9990

# =============================================================================
# ЦВЕТА ДЛЯ ВЫВОДА
# =============================================================================
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# =============================================================================
# ФУНКЦИИ
# =============================================================================
print_step() {
    echo -e "${BLUE}➜${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_header() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${GREEN}$1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# =============================================================================
# ОСНОВНОЙ СКРИПТ
# =============================================================================
print_header "🏠 ЛОКАЛЬНЫЙ ДЕПЛОЙ НА WILDFLY"

# Проверка наличия WildFly
if [ ! -d "$WILDFLY_HOME" ]; then
    print_error "WildFly не найден в: $WILDFLY_HOME"
    echo "Укажите правильный путь в переменной WILDFLY_HOME"
    exit 1
fi

# Шаг 1: Сборка проекта
print_step "Сборка проекта..."
./gradlew clean build
print_success "Проект собран"

# Шаг 2: Остановка WildFly
print_step "Остановка WildFly (если запущен)..."
pkill -f "wildfly.*standalone" || true
sleep 2
print_success "WildFly остановлен"

# Шаг 3: Очистка логов и временных файлов
print_step "Очистка логов и временных файлов..."
rm -rf "$WILDFLY_HOME/standalone/log/"*
rm -rf "$WILDFLY_HOME/standalone/tmp/"*
rm -rf "$WILDFLY_HOME/standalone/data/"*
print_success "Очистка выполнена"

# Шаг 4: Удаление старых деплойментов
print_step "Удаление старых деплойментов..."
rm -f "$DEPLOYMENT_DIR/is-lab1.war"*
print_success "Старые деплойменты удалены"

# Шаг 5: Копирование WAR файла
print_step "Копирование WAR файла..."
cp build/libs/is-lab1.war "$DEPLOYMENT_DIR/"
print_success "WAR файл скопирован"

# Шаг 6: Настройка Java опций
export JAVA_OPTS="-Xms256m -Xmx512m \
-XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.security=ALL-UNNAMED \
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens=java.management/javax.management=ALL-UNNAMED \
--add-opens=java.naming/javax.naming=ALL-UNNAMED"

# Шаг 7: Запуск WildFly
print_header "🚀 ЗАПУСК WILDFLY"

echo "📊 Доступные сервисы:"
echo "   • Приложение:    http://localhost:$APP_PORT/is-lab1"
echo "   • WildFly Admin: http://localhost:$MANAGEMENT_PORT"
echo ""
echo "💡 Для остановки нажмите Ctrl+C"
echo ""
print_warning "Запуск сервера..."
echo ""

# Запуск WildFly
"$WILDFLY_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0

