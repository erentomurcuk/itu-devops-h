resource "digitalocean_droplet" "web" {
  image    = "ubuntu-18-04-x64"
  name     = "web-1"
  region   = "ams3"
  size     = "s-1vcpu-1gb"
  ssh_keys = [
    digitalocean_ssh_key.leonora.fingerprint
  ]

  connection {
    host        = self.ipv4_address
    user        = "root"
    type        = "ssh"
    # If the private key is NOT ~/.ssh/id_rsa, uncomment
    #private_key = file(var.private_key)
    timeout     = "2m"
  }

  # Upload compiled artifact
  provisioner "file" {
    source      = "../minitwit/minitwit.jar"
    destination = "/opt/minitwit.jar"
  }
  provisioner "file" {
    source      = "../minitwit/control.sh"
    destination = "/opt/control.sh"
  }

  # Start the server
  provisioner "remote-exec" {
    inline = [
      # Abort on any error
      "set -o errexit",
      # Install java
      # Something touches a apt related lockfile on boot so we need to wait for
      # it to be released
      "until apt install openjdk-17-jre-headless -y; do sleep 1; done",
      "cd /opt",
      # Start server (as root user)
      "chmod +x control.sh",
      # TODO: use a volume that persist even if we destroy droplet for data!
      # Doing it this way doesn't work because the volume is attached after
      # running this script
      #"export MINITWIT_DB_PATH=/mnt/minitwit_data/minitwit.db",
      "export MINITWIT_PORT=80",
      "nohup ./control.sh init-and-start > /tmp/out.log 2>&1 &",
      "ls -al /opt /mnt",
    ]
  }
}

resource "digitalocean_droplet" "drone" {
  image    = "ubuntu-18-04-x64"
  name     = "drone-1"
  region   = "ams3"
  size     = "s-1vcpu-1gb"
  ssh_keys = [
    digitalocean_ssh_key.leonora.fingerprint
  ]

  connection {
    host        = self.ipv4_address
    user        = "root"
    type        = "ssh"
    # If the private key is NOT ~/.ssh/id_rsa, uncomment
    #private_key = file(var.private_key)
    timeout     = "2m"
  }
}

resource "digitalocean_droplet" "monitoring" {
  image    = "ubuntu-20-04-x64"
  name     = "monitoring-1"
  region   = "ams3"
  size     = "s-1vcpu-1gb"
  ssh_keys = [
    digitalocean_ssh_key.leonora.fingerprint
  ]

  connection {
    host        = self.ipv4_address
    user        = "root"
    type        = "ssh"
    # If the private key is NOT ~/.ssh/id_rsa, uncomment
    #private_key = file(var.private_key)
    timeout     = "2m"
  }
}

resource "digitalocean_droplet" "db" {
  image    = "ubuntu-20-04-x64"
  name     = "db-1"
  region   = "ams3"
  size     = "s-1vcpu-1gb"
  ssh_keys = [
    digitalocean_ssh_key.leonora.fingerprint,
    digitalocean_ssh_key.smilla.fingerprint
  ]
}
