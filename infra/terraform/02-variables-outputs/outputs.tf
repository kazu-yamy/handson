output "hello_path" {
  description = "生成したあいさつファイルのパス"
  value       = local_file.hello.filename
}

output "pet_name" {
  description = "random_pet が作った名前"
  value       = random_pet.greeter.id
}

output "token" {
  description = "ダミーの API トークン"
  value       = var.api_token
  sensitive   = true
}
