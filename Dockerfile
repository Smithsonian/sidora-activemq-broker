### BUILD image
FROM openjdk:8-jdk-slim as builder

# create app folder for sources
RUN mkdir -p /build

WORKDIR /build

COPY target/*.jar app.jar
COPY target/config config

RUN java -Djarmode=layertools -jar app.jar extract
RUN ls -la


### RUNTIME image
FROM openjdk:8-jdk-slim as runtime

ARG USER
ARG UID
ARG GID

RUN set -eux && \
    useradd -m -s /bin/bash -u $UID $USER

#Set app home folder
ENV APP_HOME /app
#Possibility to set JVM options (https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)
ENV JAVA_OPTS=""

#Create base app folder
RUN mkdir $APP_HOME

WORKDIR $APP_HOME

#Create folder to save configuration files
#RUN mkdir $APP_HOME/config
#Create folder with application logs
#RUN mkdir $APP_HOME/logs
#Create folder for tomcat
#RUN mkdir $APP_HOME/tomcat

#COPY --from=builder /build/BOOT-INF/lib $APP_HOME/lib
#COPY --from=builder /build/META-INF $APP_HOME/META-INF
#COPY --from=builder /build/BOOT-INF/classes $APP_HOME
#COPY --from=builder /build/config $APP_HOME

# Copy exploded jar contents from builder
COPY --from=builder /build/dependencies/ $APP_HOME
COPY --from=builder /build/snapshot-dependencies/ $APP_HOME
COPY --from=builder /build/resources/ $APP_HOME
COPY --from=builder /build/application/ $APP_HOME

RUN chown -R ${UID}:${GID} $APP_HOME

USER ${UID}:${GID}

VOLUME $APP_HOME/logs
VOLUME $APP_HOME/config

EXPOSE 8484

#ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.JarLauncher