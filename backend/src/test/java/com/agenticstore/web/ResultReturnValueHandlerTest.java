package com.agenticstore.web;

import com.agenticstore.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultReturnValueHandlerTest {

    ResultReturnValueHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ResultReturnValueHandler(new ObjectMapper());
    }

    @Test
    void supportsReturnType_forResultType_returnsTrue() throws Exception {
        var param = new MethodParameter(SampleMethods.class.getMethod("returnsResult"), -1);
        assertTrue(handler.supportsReturnType(param));
    }

    @Test
    void supportsReturnType_forStringType_returnsFalse() throws Exception {
        var param = new MethodParameter(SampleMethods.class.getMethod("returnsString"), -1);
        assertFalse(handler.supportsReturnType(param));
    }

    @Test
    void handleReturnValue_ok200_writes200AndBody() throws Exception {
        var response = new MockHttpServletResponse();
        var webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeResponse(HttpServletResponse.class)).thenReturn(response);
        var mavContainer = new ModelAndViewContainer();
        var param = new MethodParameter(SampleMethods.class.getMethod("returnsResult"), -1);

        handler.handleReturnValue(Result.ok("hello"), param, mavContainer, webRequest);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("hello"));
        assertTrue(mavContainer.isRequestHandled());
    }

    @Test
    void handleReturnValue_created201_writes201AndBody() throws Exception {
        var response = new MockHttpServletResponse();
        var webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeResponse(HttpServletResponse.class)).thenReturn(response);
        var mavContainer = new ModelAndViewContainer();
        var param = new MethodParameter(SampleMethods.class.getMethod("returnsResult"), -1);

        handler.handleReturnValue(Result.created("item"), param, mavContainer, webRequest);

        assertEquals(201, response.getStatus());
        assertTrue(response.getContentAsString().contains("item"));
    }

    @Test
    void handleReturnValue_noContent204_writes204WithNoBody() throws Exception {
        var response = new MockHttpServletResponse();
        var webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeResponse(HttpServletResponse.class)).thenReturn(response);
        var mavContainer = new ModelAndViewContainer();
        var param = new MethodParameter(SampleMethods.class.getMethod("returnsResult"), -1);

        handler.handleReturnValue(Result.noContent(), param, mavContainer, webRequest);

        assertEquals(204, response.getStatus());
        assertEquals("", response.getContentAsString());
    }

    @Test
    void handleReturnValue_failure404_writes404AndErrorBody() throws Exception {
        var response = new MockHttpServletResponse();
        var webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeResponse(HttpServletResponse.class)).thenReturn(response);
        var mavContainer = new ModelAndViewContainer();
        var param = new MethodParameter(SampleMethods.class.getMethod("returnsResult"), -1);

        handler.handleReturnValue(Result.failure("Not found", 404), param, mavContainer, webRequest);

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains("Not found"));
    }

    static class SampleMethods {
        public Result<String> returnsResult() { return null; }
        public String returnsString() { return null; }
    }
}
