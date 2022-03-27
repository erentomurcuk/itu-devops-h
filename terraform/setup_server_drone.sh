#!/bin/bash

# Installs software on drone server
# First parameter is IP

IP=$1

scp ./files/install_docker.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_docker.sh \&\& ./install_docker.sh

scp ./files/install_loki_docker_driver.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_loki_docker_driver.sh \&\& ./install_loki_docker_driver.sh

scp ./files/drone/* root@$IP:/root/
ssh root@$IP cd /root \&\& docker-compose up -d

scp ./files/install_node_exporter.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_node_exporter.sh \&\& ./install_node_exporter.sh
