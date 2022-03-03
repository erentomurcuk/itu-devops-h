#!/bin/bash

# Installs openjdk 17 and maven 3.8.4
# Maven is only available after running this script, not from new terminals
# Must run as root.

apt-get update
apt-get install -y openjdk-17-jdk:amd64=17.0.1+12-1~18.04.1
apt-get install -y maven:amd64=3.6.0-1~18.04.1

export PATH=/opt/apache-maven-3.8.4/bin:$PATH
