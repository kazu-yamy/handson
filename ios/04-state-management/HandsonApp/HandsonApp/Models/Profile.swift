struct Profile {
    let name: String
    let nickname: String?

    // if let: nil でなければ取り出す
    var displayName: String {
        if let nickname {
            return nickname
        }
        return name
    }

    // guard let: nil なら早期リターン
    func greeting() -> String {
        guard let nickname else {
            return "Hello, \(name)!"
        }
        return "Hello, \(nickname) (\(name))!"
    }

    // ??: nil のときの既定値
    var label: String {
        nickname ?? "(no nickname)"
    }

    // オプショナルチェーン: nil なら結果も nil
    var nicknameUppercased: String? {
        nickname?.uppercased()
    }
}
