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
  output_dir    = "${path.module}/output"
  greeting_text = var.shout ? upper(var.greeting) : var.greeting
  message       = "${local.greeting_text}, ${random_pet.greeter.id}! (by ${var.owner})\n"
}

resource "random_pet" "greeter" {
  length = var.pet_length
}

resource "local_file" "hello" {
  filename = "${local.output_dir}/hello.txt"
  content  = local.message
}

resource "local_sensitive_file" "token" {
  filename = "${local.output_dir}/token.txt"
  content  = var.api_token
}
