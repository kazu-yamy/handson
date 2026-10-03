enum ValidationError: Error, Equatable {
    case empty
    case tooLong(max: Int)
}

func validate(name: String) throws(ValidationError) {
    if name.isEmpty {
        throw .empty
    }
    if name.count > 10 {
        throw .tooLong(max: 10)
    }
}

// throws を Result に包み直した版
func validatedName(_ name: String) -> Result<String, ValidationError> {
    do {
        try validate(name: name)
        return .success(name)
    } catch {
        return .failure(error)
    }
}
