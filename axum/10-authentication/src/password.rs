use argon2::{
    Argon2,
    password_hash::{PasswordHasher, PasswordVerifier, phc::PasswordHash},
};
use std::sync::OnceLock;

use crate::error::AppError;

// パスワードを argon2id でハッシュ化し、PHC 形式の文字列（$argon2id$v=19$...）で返す。
// ソルトは hash_password がランダムに作って文字列の中に埋め込む
pub fn hash_password(password: &str) -> Result<String, argon2::password_hash::Error> {
    let hash = Argon2::default().hash_password(password.as_bytes())?;
    Ok(hash.to_string())
}

// 保存してあるハッシュ文字列とパスワードが一致するか。壊れたハッシュ文字列も false 扱い
pub fn verify_password(password: &str, hash: &str) -> bool {
    let Ok(parsed) = PasswordHash::new(hash) else {
        return false;
    };
    Argon2::default()
        .verify_password(password.as_bytes(), &parsed)
        .is_ok()
}

// 登録されていないメールでもログインのときに同じ時間をかけるための、ダミーのハッシュ。
// 毎回計算すると無駄なので、最初に使うときに 1 回だけ作る
fn dummy_hash() -> &'static str {
    static DUMMY: OnceLock<String> = OnceLock::new();
    DUMMY.get_or_init(|| hash_password("dummy password for timing").expect("hash dummy password"))
}

// サーバーの起動時に呼ぶ。ダミーのハッシュを先に作っておかないと、起動後に最初に来た
// 「未登録のメール」のログインだけ、ダミーを作る分（argon2 1 回ぶん）余計に時間がかかってしまう
pub fn warm_up() {
    dummy_hash();
}

// argon2 は CPU を十数ミリ秒占有する。async のハンドラの中で直接呼ぶと、
// その間そのワーカースレッドで他のリクエストが進まないので、spawn_blocking の専用スレッドに逃がす
pub async fn hash_password_blocking(password: String) -> Result<String, AppError> {
    tokio::task::spawn_blocking(move || hash_password(&password))
        .await
        .map_err(|error| AppError::Internal(format!("hash task failed: {error}")))?
        .map_err(|error| AppError::Internal(format!("hash failed: {error}")))
}

// hash が None（未登録のメール）のときは、ダミーのハッシュで検証して時間をそろえ、必ず false を返す
pub async fn verify_password_blocking(
    password: String,
    hash: Option<String>,
) -> Result<bool, AppError> {
    tokio::task::spawn_blocking(move || match hash {
        Some(hash) => verify_password(&password, &hash),
        None => {
            verify_password(&password, dummy_hash());
            false
        }
    })
    .await
    .map_err(|error| AppError::Internal(format!("verify task failed: {error}")))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hash_has_phc_format_and_differs_every_time() {
        let first = hash_password("correct horse battery").unwrap();
        let second = hash_password("correct horse battery").unwrap();
        println!("{first}");
        println!("{second}");
        assert!(first.starts_with("$argon2id$v=19$m=19456,t=2,p=1$"));
        assert_ne!(first, second); // ソルトが毎回違うので、同じパスワードでも別のハッシュになる
    }

    // 計測用（`cargo test --lib measure -- --ignored --nocapture`）。判定はしない
    #[test]
    #[ignore]
    fn measure_hash_and_verify_time() {
        use std::time::Instant;
        let start = Instant::now();
        let hash = hash_password("correct horse battery").unwrap();
        println!("hash:   {:?}", start.elapsed());
        let start = Instant::now();
        verify_password("correct horse battery", &hash);
        println!("verify: {:?}", start.elapsed());
    }

    #[tokio::test]
    async fn blocking_wrappers_work_and_unknown_hash_is_false() {
        let hash = hash_password_blocking("correct horse battery".to_string())
            .await
            .unwrap();
        let verify = |password: &str, hash: Option<String>| {
            verify_password_blocking(password.to_string(), hash)
        };
        assert!(
            verify("correct horse battery", Some(hash.clone()))
                .await
                .unwrap()
        );
        assert!(!verify("wrong password", Some(hash)).await.unwrap());
        assert!(!verify("correct horse battery", None).await.unwrap());
    }

    #[test]
    fn verify_accepts_right_password_and_rejects_wrong_one() {
        let hash = hash_password("correct horse battery").unwrap();
        assert!(verify_password("correct horse battery", &hash));
        assert!(!verify_password("wrong password", &hash));
        assert!(!verify_password(
            "correct horse battery",
            "not a phc string"
        ));
    }
}
