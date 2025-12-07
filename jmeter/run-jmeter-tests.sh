#!/bin/bash
# Скрипт для запуска JMeter тестов
# Использование: ./run-jmeter-tests.sh [test-type]
# test-type: light, medium, heavy, stress

set -e

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

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

# Переходим в директорию jmeter
cd "$(dirname "$0")"

# Проверка наличия JMeter
if ! command -v jmeter &> /dev/null; then
    print_error "JMeter не установлен!"
    echo ""
    echo "Установите JMeter:"
    echo "  macOS: brew install jmeter"
    echo "  Linux: sudo apt install jmeter"
    echo "  Или скачайте с https://jmeter.apache.org/download_jmeter.cgi"
    exit 1
fi

print_success "JMeter найден: $(jmeter --version | head -1)"

# Проверка доступности сервера
print_step "Проверка доступности API..."
if curl -s -f http://localhost:28600/is-lab1/api/flats?page=0&size=1 > /dev/null; then
    print_success "API сервер доступен"
else
    print_warning "API сервер недоступен на http://localhost:28600"
    echo "Запустите сервер перед тестированием!"
    read -p "Продолжить всё равно? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Определяем тип теста
TEST_TYPE=${1:-medium}

print_header "🚀 ЗАПУСК JMETER ТЕСТОВ - ${TEST_TYPE^^}"

# Создаём директорию для результатов
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="results/${TEST_TYPE}_${TIMESTAMP}"
mkdir -p "$RESULTS_DIR"

print_step "Результаты будут сохранены в: $RESULTS_DIR"

# Запуск тестов
case $TEST_TYPE in
    light)
        print_step "Лёгкая нагрузка: 10 потоков, 5 итераций..."
        jmeter -n -t API-Load-Test.jmx \
            -l "$RESULTS_DIR/results.jtl" \
            -e -o "$RESULTS_DIR/html-report" \
            -JnumThreads=10 \
            -JloopCount=5
        ;;
    medium)
        print_step "Средняя нагрузка: 50 потоков, 10 итераций..."
        jmeter -n -t API-Load-Test.jmx \
            -l "$RESULTS_DIR/results.jtl" \
            -e -o "$RESULTS_DIR/html-report"
        ;;
    heavy)
        print_step "Высокая нагрузка: 200 потоков, 20 итераций..."
        jmeter -n -t API-Load-Test.jmx \
            -l "$RESULTS_DIR/results.jtl" \
            -e -o "$RESULTS_DIR/html-report" \
            -JnumThreads=200 \
            -JloopCount=20
        ;;
    stress)
        print_step "Стресс-тест: 500 потоков, 5 итераций (spike)..."
        jmeter -n -t API-Load-Test.jmx \
            -l "$RESULTS_DIR/results.jtl" \
            -e -o "$RESULTS_DIR/html-report" \
            -JnumThreads=500 \
            -JloopCount=5 \
            -JrampUp=5
        ;;
    gui)
        print_step "Запуск в GUI режиме..."
        jmeter -t API-Load-Test.jmx
        exit 0
        ;;
    *)
        print_error "Неизвестный тип теста: $TEST_TYPE"
        echo ""
        echo "Доступные типы:"
        echo "  light  - лёгкая нагрузка (10 потоков)"
        echo "  medium - средняя нагрузка (50 потоков) [по умолчанию]"
        echo "  heavy  - высокая нагрузка (200 потоков)"
        echo "  stress - стресс-тест (500 потоков)"
        echo "  gui    - запуск в GUI режиме"
        exit 1
        ;;
esac

# Проверка результатов
if [ -f "$RESULTS_DIR/results.jtl" ]; then
    print_success "Тесты завершены!"
    echo ""
    print_step "Анализ результатов:"
    
    # Простая статистика из JTL файла
    TOTAL=$(grep -c "^[0-9]" "$RESULTS_DIR/results.jtl" || echo "0")
    ERRORS=$(grep -c "false" "$RESULTS_DIR/results.jtl" || echo "0")
    SUCCESS=$((TOTAL - ERRORS))
    
    if [ "$TOTAL" -gt 0 ]; then
        ERROR_RATE=$(awk "BEGIN {printf \"%.2f\", ($ERRORS/$TOTAL)*100}")
        SUCCESS_RATE=$(awk "BEGIN {printf \"%.2f\", ($SUCCESS/$TOTAL)*100}")
        
        echo "  📊 Всего запросов: $TOTAL"
        echo "  ✅ Успешных: $SUCCESS ($SUCCESS_RATE%)"
        echo "  ❌ Ошибок: $ERRORS ($ERROR_RATE%)"
        echo ""
        
        if [ "$ERRORS" -gt 0 ]; then
            if (( $(echo "$ERROR_RATE > 5" | bc -l) )); then
                print_warning "Высокий процент ошибок! Проверьте логи сервера."
            else
                print_warning "Есть ошибки, но процент приемлемый."
            fi
        else
            print_success "Все запросы выполнены успешно!"
        fi
    fi
    
    echo ""
    print_step "HTML отчёт сгенерирован:"
    echo "  file://$(pwd)/$RESULTS_DIR/html-report/index.html"
    echo ""
    
    # Автоматически открыть отчёт
    read -p "Открыть HTML отчёт в браузере? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if command -v open &> /dev/null; then
            open "$RESULTS_DIR/html-report/index.html"
        elif command -v xdg-open &> /dev/null; then
            xdg-open "$RESULTS_DIR/html-report/index.html"
        else
            print_warning "Не удалось открыть браузер автоматически"
        fi
    fi
else
    print_error "Тесты завершились с ошибкой!"
    exit 1
fi

print_header "✨ ГОТОВО!"

