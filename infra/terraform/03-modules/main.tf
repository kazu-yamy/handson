terraform {
  required_version = ">= 1.9"

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
