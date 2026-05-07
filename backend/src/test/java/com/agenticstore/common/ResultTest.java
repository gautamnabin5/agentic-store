package com.agenticstore.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void ok_wrapsValueWith200() {
        Result<String> result = Result.ok("hello");
        assertInstanceOf(Result.Success.class, result);
        assertEquals("hello", ((Result.Success<String>) result).value());
        assertEquals(200, ((Result.Success<String>) result).httpStatus());
    }

    @Test
    void created_wrapsValueWith201() {
        Result<String> result = Result.created("hello");
        assertInstanceOf(Result.Success.class, result);
        assertEquals("hello", ((Result.Success<String>) result).value());
        assertEquals(201, ((Result.Success<String>) result).httpStatus());
    }

    @Test
    void noContent_wrapsNullWith204() {
        Result<Void> result = Result.noContent();
        assertInstanceOf(Result.Success.class, result);
        assertNull(((Result.Success<Void>) result).value());
        assertEquals(204, ((Result.Success<Void>) result).httpStatus());
    }

    @Test
    void success_isAliasForOk() {
        Result<String> result = Result.success("ok");
        assertInstanceOf(Result.Success.class, result);
        assertEquals(200, ((Result.Success<String>) result).httpStatus());
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
        Result<String> ok = Result.ok("ok");
        Result<String> err = Result.failure("err", 500);
        assertInstanceOf(Result.Success.class, ok);
        assertInstanceOf(Result.Failure.class, err);
    }
}
