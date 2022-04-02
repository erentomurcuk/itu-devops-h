#!/bin/bash

# Installs software on db server
# First parameter is IP

IP=$1

scp ./files/install_docker.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_docker.sh \&\& ./install_docker.sh

ssh root@$IP mkdir -p /opt/postgres
scp ./files/db/docker-compose.yml root@$IP:/opt/postgres/docker-compose.yml
# TODO: fix user to have same UID as container user
ssh root@$IP mkdir -p /mnt/minitwitdb/data
#ssh root@$IP chmod --recursive g+rw /opt/grafana
#ssh root@$IP chown nobody:root /opt/prometheus
ssh root@$IP cd /opt/postgres \&\& docker-compose up -d

scp ./files/install_node_exporter.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_node_exporter.sh \&\& ./install_node_exporter.sh
