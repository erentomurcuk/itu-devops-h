variable "do_token" {}
variable "access_id" {}
variable "secret_key" {}
#variable "private_key" {}

terraform {
  required_providers {
    digitalocean = {
      source = "digitalocean/digitalocean"
      version = "2.17.1"
    }
  }
  backend "s3" {
    skip_credentials_validation = true
    skip_metadata_api_check = true
    # Vars not allowed, see resource bellow for value
    endpoint = "https://ams3.digitaloceanspaces.com"
    # Terraform assumes valid AWS S3 values, DO will ignore this and use endpoint
    region = "us-east-1"
    # Vars not allowed, see resource bellow for value
    bucket = "minitweit-terraform-backend"
    key = "production/terraform.tfstate"
  }
}

provider "digitalocean" {
  token             = var.do_token
  spaces_access_id  = var.access_id
  spaces_secret_key = var.secret_key
}

resource "digitalocean_spaces_bucket" "terraform-backend" {
  name   = "minitweit-terraform-backend"
  region = "ams3"
  acl    = "private"
}

resource "digitalocean_ssh_key" "leonora" {
  name       = "leonora"
  public_key = file("keys/id_rsa_leonora.pub")
}

# Drone ssh key
resource "digitalocean_ssh_key" "drone" {
  name       = "drone"
  public_key = file("files/drone/id_rsa.pub")
}

resource "digitalocean_ssh_key" "smilla" {
  name       = "smilla"
  public_key = file("keys/id_rsa_smilla.pub")
}

#resource "digitalocean_ssh_key" "eren" {
#  name       = "eren"
#  public_key = "file(keys/id_rsa_eren.pub)"
#}

resource "digitalocean_ssh_key" "lucas" {
  name       = "lucas"
  public_key = file("keys/id_rsa_lucas.pub")
}

resource "digitalocean_ssh_key" "simon" {
  name       = "simon"
  public_key = file("keys/id_rsa_simon.pub")
}
