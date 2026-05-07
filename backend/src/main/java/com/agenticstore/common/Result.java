package com.agenticstore.common;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {}

    record Failure<T>(String error, int httpStatus) implements Result<T> {}

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String error, int httpStatus) {
        return new Failure<>(error, httpStatus);
    }
}
