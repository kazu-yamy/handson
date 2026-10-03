import Foundation
import SwiftData

@Model
final class TodoRecord {
    // サーバー側の id。同じ値の行は 1 件だけにする（取り込みを繰り返しても重複しない）
    @Attribute(.unique) var remoteID: Int?
    var title: String
    var priority: Priority
    var isDone: Bool
    var completedAt: Date?
    var createdAt: Date
    // 場所。どちらも nil なら「場所なし」。あとから追加した省略可能なプロパティなので、
    // 既存のデータは nil のまま開ける（SwiftData の軽量マイグレーション）
    var latitude: Double?
    var longitude: Double?

    init(title: String, priority: Priority = .medium, createdAt: Date = .now, remoteID: Int? = nil) {
        self.remoteID = remoteID
        self.title = title
        self.priority = priority
        self.isDone = false
        self.completedAt = nil
        self.createdAt = createdAt
    }

    // 緯度と経度が両方そろっているときだけ場所があるとみなす
    var hasLocation: Bool {
        latitude != nil && longitude != nil
    }

    func setLocation(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }

    func clearLocation() {
        latitude = nil
        longitude = nil
    }

    // 完了と未完了を切り替える。完了にした時刻も一緒に記録する
    func toggle() {
        isDone.toggle()
        completedAt = isDone ? .now : nil
    }
}
