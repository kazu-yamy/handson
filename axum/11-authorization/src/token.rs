use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use sha2::{Digest, Sha256};

// 32 バイトの乱数を base64url（パディング無し、43 文字）にした、意味を持たない（不透明な）トークンを作る
pub fn generate_token() -> Result<String, getrandom::Error> {
    let mut bytes = [0u8; 32];
    getrandom::fill(&mut bytes)?;
    Ok(URL_SAFE_NO_PAD.encode(bytes))
}

// DB にはトークンそのものではなく SHA-256（32 バイト）だけを保存する。
// 乱数が十分に長いので、パスワードのような遅いハッシュ（argon2）は要らない
pub fn hash_token(token: &str) -> Vec<u8> {
    Sha256::digest(token.as_bytes()).to_vec()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn token_is_43_url_safe_characters_and_never_repeats() {
        let first = generate_token().unwrap();
        let second = generate_token().unwrap();
        assert_eq!(first.len(), 43);
        assert!(
            first
                .chars()
                .all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_')
        );
        assert_ne!(first, second);
    }

    #[test]
    fn hash_is_sha256_of_the_token_string() {
        // SHA-256("abc") の既知の値（NIST のテストベクトル）
        let hash = hash_token("abc");
        let hex: String = hash.iter().map(|byte| format!("{byte:02x}")).collect();
        assert_eq!(
            hex,
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
    }
}
