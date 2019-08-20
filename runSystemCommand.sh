#!/usr/bin/env bash

################################################
# Usage: ./runSystemCommand.sh <command name>
# Where command is one of CATCHUP, INDEXER_CATCHUP, SHUTTER, PING, REBUILD, UNSHUTTER
#
# To list all commands:
#   ./runSystemCommand.sh
#
################################################

FRAMEWORK_JMX_COMMAND_CLIENT_VERSION=2.0.1
CONTEXT_NAME="sjp-service"
USER_NAME="admin"
PASSWORD="admin"

#fail script on error
set -e

echo
echo "Framework System Command Client"
echo "Downloading artifacts..."
echo
mvn --quiet org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice:framework-jmx-command-client:${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}:jar

if [ -z "$1" ]; then
  echo "Listing commands"
  echo
  java -jar target/framework-jmx-command-client-${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}.jar -l -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
else
  COMMAND=$1
  echo "Running command '$COMMAND'"
  echo
  java -jar target/framework-jmx-command-client-${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}.jar -c "$COMMAND" -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
fi
