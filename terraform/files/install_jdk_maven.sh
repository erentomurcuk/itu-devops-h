#!/bin/bash

# Installs openjdk 17 and maven 3.8.4
# Maven is only available after running this script, not from new terminals
# Must run as root.

apt-get update
until apt-get install -y \
    "openjdk-17-jdk:amd64=17.*" \
    "openjdk-17-jre:amd64=17.*"; do sleep 1; done

wget -O "/tmp/maven.tar.gz" https://dlcdn.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz
tar zxvf /tmp/maven.tar.gz -C /opt
cp /opt/apache-maven-3.8.4/bin/* /usr/share/bin/
