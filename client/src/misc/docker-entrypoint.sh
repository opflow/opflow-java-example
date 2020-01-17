#!/bin/sh

java -Dlog4j.configuration=file:/app/config/log4j.properties \
     -Dopflow.configuration=file:/app/config/client.properties \
     -jar /app/opflow-java-sample-client.jar $@
