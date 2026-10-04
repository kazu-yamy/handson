variable "greeting" {
  type        = string
  description = "ファイルに書くあいさつ文"
  default     = "Hello"
}

variable "owner" {
  type        = string
  description = "ファイルの作成者名（default なし。必ず値を渡す）"
}

variable "pet_length" {
  type        = number
  description = "random_pet の名前を構成する単語の数（1〜5）"
  default     = 2

  validation {
    condition     = var.pet_length >= 1 && var.pet_length <= 5
    error_message = "pet_length は 1 以上 5 以下で指定してください。"
  }
}

variable "shout" {
  type        = bool
  description = "true にするとあいさつ文を大文字にする"
  default     = false
}

variable "api_token" {
  type        = string
  description = "ダミーの API トークン（機微な値の例）"
  sensitive   = true
  default     = "dummy-token-12345"
}
