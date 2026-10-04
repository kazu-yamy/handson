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

resource "random_pet" "greeter" {
  length = 2
}

resource "local_file" "hello" {
  filename = "${path.module}/output/hello.txt"
  content  = "Hello, ${random_pet.greeter.id}!\n"
}
