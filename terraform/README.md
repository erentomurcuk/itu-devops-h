# Digital Ocean infrastructure provisioning with terraform

Before doing anything, make sure your public key is in the keys folder and in the provider.tf file.

To change infrastructure it's easiest if you use env variables to store the DO access token. Run:
```
  export do_token=your_pat
  export SPACES_ACCESS_KEY_ID=...
  export SPACES_SECRET_ACCESS_KEY=.
```
where your_pat is your private token created on the Digital Ocean dashboard, found in the [API](https://cloud.digitalocean.com/account/api/tokens) menu. **Place a whitespace as the first character/in front of export to avoid saving sensitive information in your bash history**. The spaces id and key are generated on https://cloud.digitalocean.com/account/api/tokens.

If the infrastructure does not exist and you are starting from a clean state, comment out the backend block in provider.tf, run the apply command bellow, add the backend block again and run `terraform init -backend-config="access_key=${SPACES_ACCESS_KEY_ID}" -backend-config="secret_key=${SPACES_SECRET_ACCESS_KEY}"`. This is because we will be using a Spaces (S3) bucket to store the terraform state, but the bucket does not exist prior to running apply.

To create everything, run `terraform apply -var "access_id=${SPACES_ACCESS_KEY_ID}" -var "secret_key=${SPACES_SECRET_ACCESS_KEY}" -var "do_token=${DO_PAT}"`.

To destroy everything, run `terraform destroy -var "access_id=${SPACES_ACCESS_KEY_ID}" -var "secret_key=${SPACES_SECRET_ACCESS_KEY}" -var "do_token=${DO_PAT}"`. *This removes old IP address, data volumes etc irreversibly*.

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

   DRONE_USER_CREATE="username:<ADD GITHUB USER HERE>,admin:true"
   ```

  Use `openssl rand -hex 16` for a 16 byte random string.

* (Re)start drone with `docker-compose up -d`.

* Sign in on drone pipeline server, select and activate desired repository.

* Add a secret called `token` in settings with the Digital Ocean API token.

* Add a secret called `root_password` with the root password to the production servers.

* Change the configuration file path to `minitwit/.drone.yml`.

* Change project visibility to "internal".

* (Optional) start a build by pressing the "New Build" button and choose appropriate branch that has a `.drone.yml` in minitwit.

* Change project to be protected. [Update the signature if required](https://docs.drone.io/signature/).

* Make the project trusted in order to allow priviledged pipeline steps.

  Go to repository settings ad a administrator account and enable it.

### Failed build

Sometimes drone fails without explaination or during java installation. Try rerun it a 2-3 times before debuging it.

If a build takes more than 5 minutes (at the time of writing) then it's likely to be stuck. Cancel it and restart it. Drone will automatically terminate very long running builds.

## Prometheus

Will be accessible from the droplet ip on port 9090.

## Grafana

Will be accessible from the droplet ip on port 3000. On first setup, go to the front page and log in with admin/admin, and change the password.

Dashboards are added manually, but there's a collection of "standard" dashboards on https://grafana.com/grafana/dashboards. See

* [Prometheus 2.0 Overview](https://grafana.com/grafana/dashboards/3662)
* [Node Exporter Full](https://grafana.com/grafana/dashboards/1860)

After setting up a useful dashboard, consider saving its JSON Model so it can easily be recreated if the data is lost in the [dashboards folder](files/monitoring/dashboards/).

At the time of writing Grafana needs these datasources:

* The Prometheus server.

* JSON API from 1 minitwit server.

  Consider replacing this with a PostgreSQL data source.

Consider adding a alert contact point and set up alerts.

## Postgres database

Install is done with the terraform/setup_server_db.sh script, but a .env file must be created manually in /opt/postgres with a POSTGRES_PASSWORD value, which must also be created in Drone as a secret for minitwit server deployments to be successful.
