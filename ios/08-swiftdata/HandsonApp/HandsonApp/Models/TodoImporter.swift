import Foundation
import SwiftData

// API から取ってきた 1 件ずつを TodoRecord として保存する。
// メインアクター上の modelContext で行う（件数が少なく、@ModelActor で別スレッドに逃がす必要がないため）
func importTodos(_ dtos: [TodoDTO], into context: ModelContext) throws {
    for dto in dtos {
        let record = TodoRecord(title: dto.title, remoteID: dto.id)
        if dto.completed {
            record.toggle()
        }
        // remoteID が同じ行が既にあれば、insert は新規追加ではなく上書きになる（.unique の挙動）
        context.insert(record)
    }
    try context.save()
}
