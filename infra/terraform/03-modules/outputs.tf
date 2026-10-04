output "hello_path" {
  description = "生成したあいさつファイルのパス（キーごと）"
  value       = { for name, m in module.greeting : name => m.hello_path }
}

output "pet_name" {
  description = "random_pet が作った名前（キーごと）"
  value       = { for name, m in module.greeting : name => m.pet_name }
}

output "token" {
  description = "ダミーの API トークン"
  value       = var.api_token
  sensitive   = true
}
