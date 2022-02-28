terraform {
  required_providers {
    digitalocean = {
      source = "digitalocean/digitalocean"
      version = "2.17.1"
    }
  }
}

variable "do_token" {}
#variable "private_key" {}

provider "digitalocean" {
  token = var.do_token
}

resource "digitalocean_ssh_key" "leonora" {
  name       = "leonora"
  public_key = file("keys/id_rsa_leonora.pub")
}

resource "digitalocean_ssh_key" "smilla" {
  name       = "smilla"
  public_key = file("keys/id_rsa_smilla.pub")
}
