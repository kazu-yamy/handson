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

    init(title: String, priority: Priority = .medium, createdAt: Date = .now, remoteID: Int? = nil) {
        self.remoteID = remoteID
        self.title = title
        self.priority = priority
        self.isDone = false
        self.completedAt = nil
        self.createdAt = createdAt
    }

    // 完了と未完了を切り替える。完了にした時刻も一緒に記録する
    func toggle() {
        isDone.toggle()
        completedAt = isDone ? .now : nil
    }
}
