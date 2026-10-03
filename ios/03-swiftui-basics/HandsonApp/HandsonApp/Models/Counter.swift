// 値型: 代入するとコピーされる
struct Counter {
    var count = 0

    mutating func increment() {
        count += 1
    }
}

// 参照型: 代入しても同じインスタンスを指す
final class CounterBox {
    var count = 0

    func increment() {
        count += 1
    }
}
