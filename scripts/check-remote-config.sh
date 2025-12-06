#!/bin/bash
# Скрипт для проверки конфигурации WildFly на удаленном сервере

echo "=== Проверка конфигурации WildFly на удаленном сервере ==="
echo ""

ssh ifmo << 'EOF'
WILDFLY_PATH="~/Web/lab3/wildfly-preview-26.1.3.Final"
STANDALONE_XML="$WILDFLY_PATH/standalone/configuration/standalone.xml"

echo "1️⃣  Проверка наличия PostgreSQL драйвера..."
if [ -f "$WILDFLY_PATH/modules/system/layers/base/org/postgresql/main/module.xml" ]; then
    echo "✅ PostgreSQL модуль найден"
else
    echo "❌ PostgreSQL модуль НЕ найден"
    echo "   Нужно установить PostgreSQL драйвер в WildFly"
fi

echo ""
echo "2️⃣  Проверка DataSource в standalone.xml..."
if grep -q "flatsPu" "$STANDALONE_XML" 2>/dev/null; then
    echo "✅ DataSource 'flatsPu' найден в standalone.xml"
    grep -A 5 "flatsPu" "$STANDALONE_XML" | head -6
else
    echo "❌ DataSource 'flatsPu' НЕ найден в standalone.xml"
    echo ""
    echo "📝 Нужно добавить в standalone.xml:"
    echo ""
    cat << 'DSXML'
<datasource jndi-name="java:jboss/datasources/flatsPu" 
            pool-name="flatsPu" 
            enabled="true">
    <connection-url>jdbc:postgresql://pg:5432/studs</connection-url>
    <driver>postgresql</driver>
    <security>
        <user-name>ВАШ_USER</user-name>
        <password>ВАШ_ПАРОЛЬ</password>
    </security>
</datasource>
DSXML
fi

echo ""
echo "3️⃣  Проверка версии Java..."
java -version 2>&1 | head -1

echo ""
echo "4️⃣  Проверка свободного места..."
df -h ~ | tail -1

echo ""
echo "=== Проверка завершена ==="
EOF

