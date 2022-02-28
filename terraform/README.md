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
