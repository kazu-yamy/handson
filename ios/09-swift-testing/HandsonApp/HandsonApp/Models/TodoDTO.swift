import Foundation

// JSONPlaceholder の /todos が返す 1 件分の JSON に対応する型
nonisolated struct TodoDTO: Codable {
    let userId: Int
    let id: Int
    let title: String
    let completed: Bool
}

nonisolated extension TodoItem {
    // API の 1 件から画面用のモデルを作る。優先度は API に無いので既定値（中）
    init(dto: TodoDTO) {
        self.init(title: dto.title, status: dto.completed ? .done(Date()) : .todo)
    }
}
