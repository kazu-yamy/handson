terraform {
  required_providers {
    local = {
      source  = "hashicorp/local"
      version = ">= 2.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.0"
    }
  }
}

locals {
  greeting_text = var.shout ? upper(var.greeting) : var.greeting
  message       = "${local.greeting_text}, ${random_pet.greeter.id}! (by ${var.owner})\n"
}

resource "random_pet" "greeter" {
  length = var.pet_length
}

resource "local_file" "hello" {
  filename = "${var.output_dir}/${var.file_name}"
  content  = local.message
}
