#!/bin/bash

if [ -z $MINITWIT_DB_PATH ]
then
    export MINITWIT_DB_PATH="minitwit.db"
fi

function init_db {
    java -cp minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar:. SQLite
}
function start {
    java -jar minitwit-1.0-SNAPSHOT-jar-with-dependencies.jar
}

if [ "$1" = "init-and-start" ]
then
    if [ ! -e $MINITWIT_DB_PATH ]
    then
        echo "No database file found, initializing..."
        init_db
    fi
    start
fi
