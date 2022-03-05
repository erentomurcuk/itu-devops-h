#!/bin/bash

# Installs openjdk 17 and maven 3.8.4
# Maven is only available after running this script, not from new terminals
# Must run as root.

echo "Updating..."

until apt-get update; do sleep 1; done
until apt-get upgrade -y; do sleep 1; done

echo "Installing openjdk-17-jdk and openjdk-17-jre"
until apt-get install -y "openjdk-17-jre"; do sleep 1; done
until apt-get install -y "openjdk-17-jdk"; do sleep 1; done

echo "Downloading maven"
# Note: default download server (dlcdn.apache.org) returns 503 to Digital Ocean IPs at the moment
# 2022-03-03
wget -O "/tmp/maven.tar.gz" https://downloads.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz
echo "Unpacking maven"
tar zxvf /tmp/maven.tar.gz -C /opt
echo "Adding maven bin path to PATH"
export PATH=$PATH:/opt/apache-maven-3.8.4/bin
echo "export PATH=$PATH:/opt/apache-maven-3.8.4/bin" > ~/.profile
