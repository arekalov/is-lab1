#!/bin/bash
# Файл: deploy.sh

./gradlew clean build
./clean_server.sh
ssh ifmo "pkill -9 -f java"
ssh ifmo "rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/log/*"
ssh ifmo " rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/tmp/*"
scp build/libs/is-lab1.war ifmo:~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/deployments
ssh ifmo "export JAVA_OPTS=\"-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED\"; ~/Web/lab3/wildfly-preview-26.1.3.Final/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0"
