FROM openjdk:17-jdk

MAINTAINER 胡雨軒 Петр <https://github.com/piotr-yuxuan>

COPY ./welcome-base-api.jar /opt/welcome-base-api.jar

USER nobody

ARG HOSTNAME
ENV HOSTNAME=${HOSTNAME:localhost}

ENV SERVICE_OPTS="-server -XX:+ExitOnOutOfMemoryError -Dfile.encoding=utf-8"

# For fine tuning, see https://wiki.openjdk.java.net/display/zgc/Main, and see
# conclusions of https://www.diva-portal.org/smash/get/diva2:1466940/FULLTEXT01.pdf
ENV GC_OPTS="-XX:+UseZGC -XX:MaxHeapSize=2048m -XX:-ZUncommit"

ENV DEBUG_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=$HOSTNAME"

ARG PORT
ENV PORT=${PORT:3004}
EXPOSE ${PORT}

CMD exec $JAVA_HOME/bin/java $SERVICE_OPTS $DEBUG_OPTS $JAVA_OPTS -jar /opt/welcome-base-api.jar
