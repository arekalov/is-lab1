#!/bin/bash
# Файл: deploy.sh

./gradlew clean build
./clean_server.sh
ssh ifmo "pkill -9 -f java"
ssh ifmo "rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/log/*"
ssh ifmo " rm -rf ~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/tmp/*"
scp build/libs/is-lab1.war ifmo:~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/deployments
ssh -L 8080:localhost:8080 -N -f ifmo
ssh ifmo "~/Web/lab3/wildfly-preview-26.1.3.Final/bin/standalone.sh"
