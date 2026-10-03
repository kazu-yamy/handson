import Testing

// タグは Tag の static プロパティとして宣言する。@Tag を付ける
extension Tag {
    @Tag static var validation: Self
    @Tag static var networking: Self
    @Tag static var slow: Self
}
