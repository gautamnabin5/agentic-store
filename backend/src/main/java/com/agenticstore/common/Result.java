package com.agenticstore.common;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value, int httpStatus) implements Result<T> {}

    record Failure<T>(String error, int httpStatus) implements Result<T> {}

    static <T> Result<T> ok(T value) {
        return new Success<>(value, 200);
    }

    static <T> Result<T> created(T value) {
        return new Success<>(value, 201);
    }

    static <T> Result<T> noContent() {
        return new Success<>(null, 204);
    }

    static <T> Result<T> success(T value) {
        return ok(value);
    }

    static <T> Result<T> failure(String error, int httpStatus) {
        return new Failure<>(error, httpStatus);
    }
}
