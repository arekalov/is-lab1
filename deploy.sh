./gradlew clean build
ssh ifmo "rm -rf Web/lab3/wildfly-preview-26.1.3.Final/standalone/deployments/*"
ssh ifmo "pkill -9 -f java"
scp build/libs/is-lab1.war ifmo:~/Web/lab3/wildfly-preview-26.1.3.Final/standalone/deployments
ssh -L 8080:localhost:8080 -N -f ifmo
# shellcheck disable=SC2088
ssh ifmo "~/Web/lab3/wildfly-preview-26.1.3.Final/bin/standalone.sh"

