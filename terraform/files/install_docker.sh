#!/bin/bash

# Installs docker on local system.
# Must run as root.
# Based on https://docs.docker.com/engine/install/ubuntu/#install-from-a-package

# Stop on errors
set -e

! docker version | grep " 20.10.9$" > /dev/null 2>&1 || echo "Docker is already installed" && exit 0

# Uninstall existing packages
echo "Removing any old packages"
! dpkg -l docker-ce || service docker stop && dpkg -P docker-ce
! dpkg -l docker-ce-cli || dpkg -P docker-ce-cli
! dpkg -l containerd-io || dpkg -P containerd-io

# Download relevant files, find newest at
# https://download.docker.com/linux/ubuntu/dists/focal/pool/stable/amd64/
wget -O "/tmp/containerd.io.deb" https://download.docker.com/linux/ubuntu/dists/focal/pool/stable/amd64/containerd.io_1.4.9-1_amd64.deb \
    && wget -O "/tmp/docker-ce-cli.deb" https://download.docker.com/linux/ubuntu/dists/focal/pool/stable/amd64/docker-ce-cli_20.10.9~3-0~ubuntu-focal_amd64.deb \
    && wget -O "/tmp/docker-ce.deb" https://download.docker.com/linux/ubuntu/dists/focal/pool/stable/amd64/docker-ce_20.10.9~3-0~ubuntu-focal_amd64.deb

# Install packages
dpkg -i /tmp/containerd.io.deb \
    && dpkg -i /tmp/docker-ce-cli.deb \
    && dpkg -i /tmp/docker-ce.deb

# Install docker-compose
# See version at https://docs.docker.com/compose/install/
echo "Installing docker-compose"
curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# If entropy is low causing docker-compose up to hang, this package can fix it
# https://stackoverflow.com/questions/59941911/docker-compose-up-hangs-forever-how-to-debug
echo "Installing haveged"
apt install haveged -y

# Cleanup
echo "Removing installation files"
rm /tmp/docker-ce.deb
rm /tmp/docker-ce-cli.deb
rm /tmp/containerd.io.deb
