terraform {
  required_version = ">= 1.10"

  backend "s3" {
    bucket       = "tfstate"
    key          = "handson/terraform.tfstate"
    region       = "us-east-1"
    use_lockfile = true

    endpoints = {
      s3 = "http://localhost:8333"
    }
    use_path_style              = true
    skip_credentials_validation = true
    skip_requesting_account_id  = true
    skip_metadata_api_check     = true
    skip_region_validation      = true
  }

  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "~> 2.9"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }
}

locals {
  output_dir = "${path.module}/output"

  # キーがファイル名（.txt を除く）、値があいさつ文
  greetings = {
    hello   = var.greeting
    goodbye = "Goodbye"
  }
}

module "greeting" {
  source   = "./modules/greeting"
  for_each = local.greetings

  greeting   = each.value
  owner      = var.owner
  pet_length = var.pet_length
  shout      = var.shout
  output_dir = local.output_dir
  file_name  = "${each.key}.txt"
}

resource "local_sensitive_file" "token" {
  filename = "${local.output_dir}/token.txt"
  content  = var.api_token
}
