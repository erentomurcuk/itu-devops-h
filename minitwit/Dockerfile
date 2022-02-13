FROM maven:3.8.4-openjdk-17-slim
COPY . /usr/src/minitwit
WORKDIR /usr/src/minitwit
RUN ./control.sh build


FROM openjdk:17-slim
RUN useradd minitwit && \
    mkdir /app && \
    mkdir /data && \
    chown -R minitwit:minitwit /data
USER minitwit
WORKDIR /app
ENV MINITWIT_DB_PATH=/data/minitwit.db
COPY --from=0 /usr/src/minitwit/minitwit.jar ./
COPY --from=0 /usr/src/minitwit/control.sh ./
EXPOSE 8080
ENTRYPOINT [ "./control.sh", "init-and-start" ]
