#!/bin/sh

# Gradle wrapper for cypy (Android)
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Use default JVM opts
DEFAULT_JVM_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"

DIRNAME=$(dirname "$0")
APP_HOME=$(cd "$DIRNAME" && pwd)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine Java command
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
