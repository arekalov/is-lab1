#!/bin/bash
# Удаленный деплой и запуск WildFly на сервере ITMO
# Использование: ./deploy-remote.sh

set -e  # Остановка при ошибке

# =============================================================================
# КОНФИГУРАЦИЯ
# =============================================================================
REMOTE_HOST="ifmo"
REMOTE_WILDFLY_PATH="~/Web/lab3/wildfly-preview-26.1.3.Final"
REMOTE_DEPLOY_DIR="$REMOTE_WILDFLY_PATH/standalone/deployments"

# Порты для проброски
APP_PORT=8080
HTTP_MANAGEMENT_PORT=28600
MANAGEMENT_PORT=28603

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
print_header "🌐 УДАЛЕННЫЙ ДЕПЛОЙ НА СЕРВЕР ITMO"

# Проверка SSH соединения
print_step "Проверка соединения с сервером..."
if ! ssh -o ConnectTimeout=5 "$REMOTE_HOST" "echo 'OK'" &>/dev/null; then
    print_error "Не удалось подключиться к серверу $REMOTE_HOST"
    echo "Проверьте SSH конфигурацию и доступность сервера"
    exit 1
fi
print_success "Соединение с сервером установлено"

# Шаг 1: Локальная сборка проекта
print_step "Локальная сборка проекта..."
./gradlew clean build
print_success "Проект собран"

# Шаг 2: Остановка Java процессов на сервере
print_step "Остановка WildFly на сервере..."
ssh "$REMOTE_HOST" "pkill -f wildfly || true"
sleep 3
print_success "WildFly остановлен"

# Шаг 3: Очистка сервера
print_header "🧹 ОЧИСТКА СЕРВЕРА"

print_step "Проверка использования диска..."
ssh "$REMOTE_HOST" "df -h | grep -E '(Filesystem|/home)'"

print_step "Очистка WildFly временных файлов..."
ssh "$REMOTE_HOST" "
    cd $REMOTE_WILDFLY_PATH 2>/dev/null && {
        rm -rf standalone/tmp/* 2>/dev/null || true
        rm -rf standalone/data/* 2>/dev/null || true
        rm -rf standalone/log/* 2>/dev/null || true
        rm -rf standalone/configuration/standalone_xml_history/* 2>/dev/null || true
        echo 'WildFly временные файлы очищены'
    } || echo 'Каталог WildFly не найден'
"

print_step "Очистка деплойментов..."
ssh "$REMOTE_HOST" "
    cd $REMOTE_DEPLOY_DIR 2>/dev/null && {
        rm -f *.war* 2>/dev/null || true
        rm -f *.failed 2>/dev/null || true
        rm -f *.pending 2>/dev/null || true
        echo 'Деплойменты очищены'
    } || echo 'Каталог деплойментов не найден'
"

print_step "Очистка кэшей..."
ssh "$REMOTE_HOST" "
    rm -rf ~/.gradle/caches/* 2>/dev/null && echo 'Gradle кэш очищен' || true
"

print_success "Очистка сервера завершена"

# Шаг 4: Деплой WAR файла
print_step "Отправка WAR файла на сервер..."
scp build/libs/is-lab1.war "$REMOTE_HOST:$REMOTE_DEPLOY_DIR/"
print_success "WAR файл отправлен"

# Шаг 5: Запуск WildFly с логами
print_header "🚀 ЗАПУСК WILDFLY НА СЕРВЕРЕ"

echo "📊 Доступные сервисы (через проброс портов):"
echo "   • Приложение:    http://localhost:$APP_PORT/is-lab1"
echo "   • WildFly Admin: http://localhost:$HTTP_MANAGEMENT_PORT"
echo "   • Management:    http://localhost:$MANAGEMENT_PORT"
echo ""
echo "💡 Для остановки нажмите Ctrl+C"
echo ""
print_warning "Подключение к серверу и запуск WildFly..."
echo ""

# Подключение с проброской портов и запуск сервера
ssh -L "$APP_PORT:localhost:8080" \
    -L "$HTTP_MANAGEMENT_PORT:localhost:28600" \
    -L "$MANAGEMENT_PORT:localhost:28603" \
    "$REMOTE_HOST" \
    "cd $REMOTE_WILDFLY_PATH && \
     export JAVA_OPTS=\"-Xms128m -Xmx256m \
     -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128m \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens=java.base/java.io=ALL-UNNAMED \
     --add-opens=java.base/java.security=ALL-UNNAMED \
     --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
     --add-opens=java.management/javax.management=ALL-UNNAMED \
     --add-opens=java.naming/javax.naming=ALL-UNNAMED\" && \
     ./bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0"

