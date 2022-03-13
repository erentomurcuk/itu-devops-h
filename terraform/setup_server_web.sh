#!/bin/bash

# Installs software on web server
# First parameter is IP, next is NEW cleartext root password

IP=$1
ROOT_PASSWORD=`echo "$2" | mkpasswd --stdin`

scp ./files/install_docker.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_docker.sh \&\& ./install_docker.sh

ssh root@$IP "echo \"root:${ROOT_PASSWORD}\" | chpasswd -e"
ssh root@$IP "sed -i 's/PasswordAuthentication\ no/PasswordAuthentication\ yes/' /etc/ssh/sshd_config"
ssh root@$IP "service ssh reload"

scp ./files/install_node_exporter.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_node_exporter.sh \&\& ./install_node_exporter.sh
