#!/bin/bash

IMAGE=$1

echo "Logging in"
docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD" registry.digitalocean.com
echo "Pulling image $IMAGE:$DRONE_BUILD_NUMBER"
docker pull $IMAGE:$DRONE_BUILD_NUMBER
echo "Stopping and removing old container"
docker stop minitwit-server && docker rm minitwit-server
echo "Starting container with new image"
docker run --name minitwit-server -d $IMAGE:$DRONE_BUILD_NUMBER
# Based on https://medium.com/rahasak/delete-docker-image-with-all-tags-c631f6049530
# Removes all images containing the string $IMAGE except one with DRONE_BUILD_NUMBER
echo "Removing images"
docker images | \
    grep minitwit-server | \
    tr -s " " | \
    cut -d " " -f 1,2 --output-delimiter=":" | \
    grep --invert-match "$IMAGE\:$DRONE_BUILD_NUMBER" | \
    xargs -I {} docker rmi {}
