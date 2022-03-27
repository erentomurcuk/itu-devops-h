#!/bin/bash

# Installs software on web server
# First parameter is IP, next is NEW cleartext root password

IP=$1

scp ./files/install_docker.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_docker.sh \&\& ./install_docker.sh

scp ./files/install_loki_docker_driver.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_loki_docker_driver.sh \&\& ./install_loki_docker_driver.sh

ssh root@$IP mkdir -p /root/monitoring
scp ./files/monitoring/docker-compose.yml root@$IP:/root/monitoring/docker-compose.yml
scp ./files/monitoring/prometheus.yml root@$IP:/opt/prometheus.yml
scp ./files/monitoring/loki-local-config.yml root@$IP:/opt/loki-local-config.yml
scp ./files/promtail-config.yml root@$IP:/opt/promtail-config.yml

# User in container has root group, so make sure root group in host can read+write volume mounts
ssh root@$IP mkdir -p /opt/grafana /opt/prometheus
ssh root@$IP chmod --recursive g+rw /opt/grafana
ssh root@$IP chown nobody:root /opt/prometheus
ssh root@$IP cd /root/monitoring \&\& docker-compose up -d

scp ./files/install_node_exporter.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_node_exporter.sh \&\& ./install_node_exporter.sh
