package com.agenticstore.web;

import com.agenticstore.common.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

@ControllerAdvice
public class ResultResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        return switch ((Result<?>) body) {
            case Result.Success<?> s -> {
                response.setStatusCode(HttpStatusCode.valueOf(s.httpStatus()));
                yield s.value();
            }
            case Result.Failure<?> f -> {
                response.setStatusCode(HttpStatusCode.valueOf(f.httpStatus()));
                yield Map.of("error", f.error());
            }
        };
    }
}
