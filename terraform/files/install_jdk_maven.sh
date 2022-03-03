#!/bin/bash

# Installs openjdk 17 and maven 3.8.4
# Maven is only available after running this script, not from new terminals
# Must run as root.

apt-get update
apt search openjdk-17-jdk
apt-search maven
until apt-get install -y \
    "openjdk-17-jdk:amd64=17.*" \
    "maven:amd64=3.*"; do sleep 1; done

export PATH=/opt/apache-maven-3.8.4/bin:$PATH
