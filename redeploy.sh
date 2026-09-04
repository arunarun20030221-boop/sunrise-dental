#!/usr/bin/env bash
# =====================================================================================
# Rebuild the WAR and redeploy it to the local Tomcat, then wait until it answers.
#
# JAVA_HOME is pinned to JDK 17 deliberately: Homebrew's Maven pulls in the latest JDK as
# a dependency, and Spring Framework 6.1 does not support that version. Without this pin the
# build silently targets the wrong JDK.
#
# Tomcat runs on 9090, not the usual 8080: this machine already has an SSH port-forward
# bound to 8080, and requests would land on whichever of the two won the race.
# =====================================================================================
set -euo pipefail

JAVA_HOME_17=/opt/homebrew/opt/openjdk@17
TOMCAT=/opt/homebrew/opt/tomcat
WEBAPPS="$TOMCAT/libexec/webapps"
APP=sunrise-dental
PORT=9090
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$PROJECT_DIR"

echo "==> Stopping Tomcat"
JAVA_HOME="$JAVA_HOME_17" "$TOMCAT/bin/catalina" stop 2>/dev/null || true
sleep 2

echo "==> Clearing previous deployment"
rm -rf "$WEBAPPS/$APP" "$WEBAPPS/$APP.war"
: > "$TOMCAT/libexec/logs/catalina.out"

echo "==> Building WAR"
# A clean build, not an incremental one: javac skips recompilation when only the POM has
# changed, which silently drops compiler-flag changes such as -parameters.
JAVA_HOME="$JAVA_HOME_17" mvn -q -pl server clean package -DskipTests

echo "==> Deploying"
cp "server/target/$APP.war" "$WEBAPPS/"

echo "==> Starting Tomcat"
JAVA_HOME="$JAVA_HOME_17" "$TOMCAT/bin/catalina" start >/dev/null 2>&1

for i in $(seq 1 25); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/$APP/login" || echo 000)
    if [ "$code" = "200" ]; then
        echo "==> Ready: http://localhost:$PORT/$APP/login (HTTP 200 after ${i}s)"
        exit 0
    fi
    sleep 1
done

echo "==> FAILED to start. Root cause:"
grep -E "^Caused by|NoSuchBean|BeanCreation|BeanInstantiation|PSQLException|ScriptStatementFailed" \
    "$TOMCAT/libexec/logs/catalina.out" | head -5
exit 1
