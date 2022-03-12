# Digital Ocean infrastructure provisioning with terraform

Before doing anything, make sure your public key is in the keys folder and in the provider.tf file.

To change infrastructure it's easiest if you use env variables to store the DO access token. Run:
```
  export do_token=...
```
where ... is your private token created on the Digital Ocean dashboard, found in the [API](https://cloud.digitalocean.com/account/api/tokens) menu. **Place a whitespace as the first character/in front of export to avoid saving sensitive information in your bash history**.

To create everything, run `terraform apply -var "do_token=${do_token}"`.

To destroy everything, run `terraform destroy -var "do_token=${do_token}" -var "private_key=${private_key}`. *This removes old IP address and data volumes irreversably*.

To see how stuff is looking right now (computed values), run `terraform show`, you can find the IP of droplets etc. here.

To only recreate some parts of the infrastructure, "taint" them with ex. `terraform taint digitalocean_droplet.web` and then run the apply command.

# Applications

## Drone

* Generate a unencrypted ssh keypair in `files/drone/` (don't commit this to git).

* Get the server running using terraform and get it's external ip using `terraform show`.

* Run `./setup_server_drone.sh $IP` where $IP is the drone server IP. This will install docker and related drone files.

* Sign in on the server and create a `.env` file in the root folder with the following values:

   ```
   DRONE_GITHUB_CLIENT_ID= # Client ID from github
   DRONE_GITHUB_CLIENT_SECRET= # Client secret from github
   
   DRONE_RPC_SECRET= # Some secret string for internal communication
   
   DRONE_SERVER_HOST=159.223.9.94 # External ip
   DRONE_SERVER_PROTO=http # http or https
   
   DRONE_USER_FILTER=erentomurcuk,Herover,KnittedSox,pizzaluc,smilladion # Github users allowed to sign in
   
   DRONE_DATABASE_SECRET= # Database encryption secret for secrets
   
   DRONE_PRIVATE_KEY_FILE="/root/id_rsa" # Location of private ssh key inside drone server
   DRONE_PUBLIC_KEY_FILE="/root/id_rsa.pub" # Location of public ssh key
   
   DRONE_RPC_PROTO=http # Protocol for drone-pipeline-to-drone-runner
   DRONE_RPC_HOST=159.223.9.94 # IP of drone pipeline server
   
   DRONE_UI_USERNAME=root # Username for drone runner dashboard
   DRONE_UI_PASSWORD= # Password for drone runner dashboard
   ```

  Use `openssl rand -hex 16` for a 16 byte random string.

* (Re)start drone with `docker-compose up -d`.

* Sign in on drone pipeline server, select and activate desired repository.

* Add a secret called `token` in settings with the Digital Ocean API token.

* Add a secret called `root_password` with the root password to the production servers.

* Change the configuration file path to `minitwit/.drone.yml`.

* Change project visibility to "internal".

* (Optional) start a build by pressing the "New Build" button and choose appropriate branch that has a `.drone.yml` in minitwit.

### Failed build

Sometimes drone fails without explaination or during java installation. Try rerun it a 2-3 times before debuging it.

If a build takes more than 5 minutes (at the time of writing) then it's likely to be stuck. Cancel it and restart it. Drone will automatically terminate very long running builds.

## Prometheus

Will be accessible from the droplet ip on port 9090.

# Grafana

Will be accessible from the droplet ip on port 3000. On first setup, go to the front page and log in with admin/admin, and change the password.

Dashboards are added manually, but there's a collection of "standard" dashboards on https://grafana.com/grafana/dashboards.
