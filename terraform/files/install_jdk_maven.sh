#!/bin/bash

# Installs openjdk 17 and maven 3.8.4
# Maven is only available after running this script, not from new terminals
# Must run as root.

apt-get update
apt-get install -y openjdk-17-jdk:amd64=17.0.1+12-1~20.04

let maxtries=10
let tries=0
until wget -O "/tmp/maven.tar.gz" https://dlcdn.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz || [ "$tries" -eq $maxtries ]; do let a++; sleep 1; done
tar zxvf /tmp/maven.tar.gz -C /opt
ln -s /opt/apache-maven-3.8.4 /opt/maven

export PATH=/opt/apache-maven-3.8.4/bin:$PATH
