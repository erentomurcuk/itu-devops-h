#!/bin/bash

IMAGE=$1
DRONE_BUILD_NUMBER=$2
DOCKER_USERNAME=$3
DOCKER_PASSWORD=$4
POSTGRES_PASSWORD=$5

echo "Logging in to registry"
docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD" registry.digitalocean.com
echo "Pulling image $IMAGE:$DRONE_BUILD_NUMBER"
docker pull $IMAGE:$DRONE_BUILD_NUMBER
echo "Stopping and removing old container"
docker stop minitwit-server && docker rm minitwit-server
echo "Starting container with new image"
docker run \
    --name minitwit-server \
    -p 80:8080 \
    -v /opt/minitwit:/opt/minitwit \
    -e "MINITWIT_DB_PASS=$POSTGRES_PASSWORD" \
    -e "MINITWIT_DB_USER=minitwituser" \
    -e "MINITWIT_DB_URL=jdbc:postgresql://159.223.236.108:5432/minitwit" \
    --restart always \
    -d \
    $IMAGE:$DRONE_BUILD_NUMBER
# Based on https://medium.com/rahasak/delete-docker-image-with-all-tags-c631f6049530
# Removes all images containing the string $IMAGE except one with DRONE_BUILD_NUMBER
echo "Removing images"
docker images | \
    grep minitwit-server | \
    tr -s " " | \
    cut -d " " -f 1,2 --output-delimiter=":" | \
    grep --invert-match "$IMAGE\:$DRONE_BUILD_NUMBER" | \
    xargs -I {} docker rmi {}
# Logging out of registry
docker logout registry.digitalocean.com
