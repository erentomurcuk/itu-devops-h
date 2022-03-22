#!/bin/bash

# Installs Prometheus Node Exporter and starts it.
# Node exporter will not restart if it stops!

echo "Downloading node exporter"
wget -O "/tmp/node_exporter.tar.gz" https://github.com/prometheus/node_exporter/releases/download/v1.3.1/node_exporter-1.3.1.linux-amd64.tar.gz

echo "Unpacking node exporter"
tar xvfz /tmp/node_exporter.tar.gz  -C /tmp

echo "Installing node exporter"
cp /tmp/node_exporter-*.linux-amd64/node_exporter /usr/local/bin/node_exporter

echo "Stopping any running node exporter processes"
killall node_exporter

echo "Starting node exporter, see /var/log/node_exporter.log"
nohup node_exporter > /var/log/node_exporter.log 2>&1 &

echo "Done"
