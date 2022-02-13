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

function build-in-docker {
    images=$( sudo docker images | grep minitwit-builder )
    if [ -z $images ]
    then
        docker build --tag minitwit-builder -f ./Dockerfile_build .
    fi
    docker run --volume /tmp/minitwit:/out minitwit-builder
    cp /tmp/minitwit/minitwit.jar ./
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
elif [ "$1" = "build-in-docker" ]
then
    build-in-docker
elif [ "$1" = "inspectdb" ]; then
    ./flag_tool -i | less
elif [ "$1" = "flag" ]; then
    ./flag_tool "$@"
else
    cat << EOF
control.sh <command>

Commands:
init:               Initialize database, will overwrite old.
start:              Start minitwit server using jar file in this directory.
init-and-start:     Initializes the database if the database file does not exist
                    yet and start server.
build:              Build and package jar file.
build-with-docker:  Build and package jar file in a docker container, and copy jar
                    file into this directory. Useful for building without having
                    a JDK and Maven installation.
inspectdb:          Shows the contents of the database in the terminal.
flag:               Flags a specific tweet which hides it from view.
EOF
fi
