#!/bin/bash

# Installs openjdk 17 and maven 3.8.4
# Maven is only available after running this script, not from new terminals
# Must run as root.

apt update
apt install -y openjdk-17-jdk:amd64=17.0.1+12-1~20.04

wget -O "/tmp/maven.tar.gz" https://dlcdn.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz
tar zxvf /tmp/maven.tar.gz -C /opt
ln -s /opt/apache-maven-3.8.4 /opt/maven

export PATH=/opt/apache-maven-3.8.4/bin:$PATH
