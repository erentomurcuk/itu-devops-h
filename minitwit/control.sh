#!/bin/bash

if [ -z $MINITWIT_DB_PATH ]
then
    export MINITWIT_DB_PATH="minitwit.db"
fi

function init_db {
    java -cp minitwit.jar:. SQLite
}

function start {
    java -jar minitwit.jar
}

function build {
    # The output filename depends on what's in pom.xml
    mvn clean compile assembly:single && \
        cp target/minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar ./minitwit.jar
}

if [ "$1" = "init-and-start" ]
then
    if [ ! -e $MINITWIT_DB_PATH ]
    then
        echo "No database file found, initializing..."
        init_db
    fi
    start
elif [ "$1" = "init" ]
then
    init_db
elif [ "$1" = "start" ]
then
    start
elif [ "$1" = "build" ]
then
    build
else
    cat << EOF
control.sh <command>

Commands:
init:           Initialize database, will overwrite old.
start:          Start minitwit server using jar file in this directory.
init-and-start: Initializes the database if the database file does not exist
                yet and start server.
build:          Build and package jar file
EOF
fi
