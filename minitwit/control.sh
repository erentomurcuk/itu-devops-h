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
    # Check for existing minitwit-builder and delete if found
    images=$( sudo docker images | grep minitwit-builder )
    if [ -n "$images" ]
    then
        echo "Old minitwit-builder already exists, attempting to delete it..."
        docker rmi minitwit-builder -f 
    fi

    # Build java project and store it in the image
    docker build --tag minitwit-builder -f ./Dockerfile_build .
    success=$?

    # Notify user if something fails, return exit code
    if [ $success != 0 ] ;
    then
        echo ""
        echo "Done with build errors"
        exit $success
    fi

    # Run only copies final jar file to container /out directory
    docker run --rm --volume /tmp/minitwit:/out minitwit-builder
    success=$?
    cp /tmp/minitwit/minitwit.jar ./

    # Clean up
    docker rmi minitwit-builder -f

    # Echo final status message
    echo ""
    if [ $success ]
    then
        echo "Done!"
    else
        echo "Done with during java build errors"
        exit $success
    fi
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
