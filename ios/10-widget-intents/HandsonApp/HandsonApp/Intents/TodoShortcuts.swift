import AppIntents

struct TodoShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: AddTodoIntent(),
            phrases: ["\(.applicationName) に ToDo を追加"],
            shortTitle: "ToDo を追加",
            systemImageName: "plus.circle"
        )
    }
}
