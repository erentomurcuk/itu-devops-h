resource "digitalocean_floating_ip" "minitwit-external" {
  droplet_id = digitalocean_droplet.web.id
  region     = digitalocean_droplet.web.region
}
