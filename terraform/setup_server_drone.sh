#!/bin/bash

IP=$1

scp ./files/install_docker.sh root@$IP:/tmp/
ssh root@$IP cd /tmp \&\& chmod +x ./install_docker.sh \&\& ./install_docker.sh

scp ./files/drone/* root@$IP:/root/
ssh root@$IP cd /root \&\& docker-compose up -d
