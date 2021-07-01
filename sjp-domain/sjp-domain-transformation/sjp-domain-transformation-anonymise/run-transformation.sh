#!/usr/bin/env bash

TRANSFORMATION_JAR=$(echo target/sjp-domain-transformation-anonymise*.jar)

# for framework 5 services, use 5.3.1
# for framework 6 services, use 6.0.1
EVENT_TOOL_VERSION=6.4.5
EVENT_TOOL_JAR=target/dependency/event-tool-${EVENT_TOOL_VERSION}-swarm.jar

PROCESS_FILE=target/processFile
STANDALONE_XML=src/test/resources/standalone-ds.xml

echo TRANSFORMATION_JAR=${TRANSFORMATION_JAR}

[[ ! -f ${TRANSFORMATION_JAR} ]] && echo "File not found: "${TRANSFORMATION_JAR} && exit

# Download Event Tool JAR
mvn dependency:copy -Dartifact=uk.gov.justice:event-tool:${EVENT_TOOL_VERSION}:jar:swarm

touch ${PROCESS_FILE}

java -jar -Dorg.wildfly.swarm.mainProcessFile=${PROCESS_FILE} \
  -DstreamCountReportingInterval=3 \
  -DprocessAllStreams=true \
  -Devent.transformation.jar=${TRANSFORMATION_JAR} ${EVENT_TOOL_JAR} \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -c ${STANDALONE_XML} -Dswarm.http.port=18080 \
  -Dswarm.https.port=18443 \
  -Dswarm.deployment.timeout=3600 |
  tee target/transformation.log
