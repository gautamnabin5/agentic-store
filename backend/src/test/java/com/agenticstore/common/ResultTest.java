package com.agenticstore.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_wrapsValue() {
        Result<String> result = Result.success("hello");
        assertInstanceOf(Result.Success.class, result);
        assertEquals("hello", ((Result.Success<String>) result).value());
    }

    @Test
    void failure_wrapsErrorAndStatus() {
        Result<String> result = Result.failure("Not found", 404);
        assertInstanceOf(Result.Failure.class, result);
        assertEquals("Not found", ((Result.Failure<String>) result).error());
        assertEquals(404, ((Result.Failure<String>) result).httpStatus());
    }

    @Test
    void success_and_failure_areDistinctTypes() {
        Result<String> ok = Result.success("ok");
        Result<String> err = Result.failure("err", 500);
        assertInstanceOf(Result.Success.class, ok);
        assertInstanceOf(Result.Failure.class, err);
    }
}
