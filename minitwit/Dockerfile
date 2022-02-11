FROM maven:3.8.4-openjdk-17-slim
COPY . /usr/src/minitwit
WORKDIR /usr/src/minitwit
RUN mvn clean compile assembly:single


FROM openjdk:17-slim
RUN useradd minitwit && \
    mkdir /app && \
    mkdir /data && \
    chown -R minitwit:minitwit /data
USER minitwit
WORKDIR /app
ENV MINITWIT_DB_PATH=/data/minitwit.db
# The output filename depends on what's in pom.xml
COPY --from=0 /usr/src/minitwit/target/minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar ./
COPY --from=0 /usr/src/minitwit/control.sh ./
EXPOSE 8080
ENTRYPOINT [ "./control.sh", "init-and-start" ]
