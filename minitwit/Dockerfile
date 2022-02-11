FROM maven:3.8.4-openjdk-17-slim
COPY . /usr/src/minitwit
WORKDIR /usr/src/minitwit
RUN mvn clean compile assembly:single


FROM openjdk:17-slim
RUN useradd minitwit && \
    mkdir /app && \
    #touch /app/minitwit.db && \
    chown -R minitwit:minitwit /app
USER minitwit
WORKDIR /app
# The output filename depends on what's in pom.xml
COPY --from=0 /usr/src/minitwit/target/minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar ./
RUN java -cp minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar:. SQLite
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar" ]
