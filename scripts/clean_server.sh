#!/bin/bash
# Файл: cleanup_server.sh

echo "=== Начинаем очистку сервера ==="

ssh ifmo << 'EOF'
echo "1. Проверка использования диска..."
df -h
echo ""
du -sh ~/* 2>/dev/null | sort -hr | head -10

echo -e "\n2. Очистка WildFly временных файлов..."
cd ~/Web/lab3/wildfly-preview-26.1.3.Final 2>/dev/null && {
    rm -rf standalone/tmp/*
    rm -rf standalone/data/*
    rm -rf standalone/log/*
    rm -rf standalone/configuration/standalone_xml_history/*
    echo "WildFly временные файлы очищены"
} || echo "Каталог WildFly не найден"

echo -e "\n3. Очистка деплойментов..."
cd ~/Web/lab3/wildfly-preview-26.1.3.Final 2>/dev/null && {
    rm -f standalone/deployments/*.war*
    rm -f standalone/deployments/*.failed
    rm -f standalone/deployments/*.pending
    echo "Деплойменты очищены"
} || echo "Каталог деплойментов не найден"

echo -e "\n4. Очистка кэшей..."
rm -rf ~/.gradle/caches/* 2>/dev/null && echo "Gradle кэш очищен"
rm -rf ~/.m2/repository/* 2>/dev/null && echo "Maven репозиторий очищен"

echo -e "\n5. Поиск больших файлов..."
find ~ -type f -size +100M -ls 2>/dev/null | head -10

echo -e "\n6. Очистка корзины..."
rm -rf ~/.Trash/* 2>/dev/null && echo "Корзина очищена"

echo -e "\n7. Итоговое использование диска:"
du -sh ~ 2>/dev/null
EOF

echo "=== Очистка завершена ==="