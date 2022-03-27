# Install grafanas loki-docker-drive plugin for docker
# TODO: It will fail if its already installed, regardless of version. Check if it exists and needs upgrading
# https://grafana.com/docs/loki/latest/clients/docker-driver/#upgrading

docker plugin install grafana/loki-docker-driver:2.4.2 --alias loki --grant-all-permissions || exit 0
