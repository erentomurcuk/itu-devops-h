# The name and id might be used as paths in Linux, but sometimes it replaces -
# with _ so just start with _ to avoid discrepencies
#resource "digitalocean_volume" "minitwit_data" {
#  region                  = "ams3"
#  name                    = "minitwit_data"
#  size                    = 1
#  initial_filesystem_type = "ext4"
#  description             = "Contains minitwit persistent storage"
#}

#resource "digitalocean_volume_attachment" "web-data" {
#  droplet_id = digitalocean_droplet.web.id
#  volume_id  = digitalocean_volume.minitwit-data.id
#}
