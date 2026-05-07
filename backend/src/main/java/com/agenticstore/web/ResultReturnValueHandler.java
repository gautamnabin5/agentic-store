package com.agenticstore.web;

import com.agenticstore.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

public class ResultReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final ObjectMapper objectMapper;

    public ResultReturnValueHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        mavContainer.setRequestHandled(true);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

        switch ((Result<?>) returnValue) {
            case Result.Success<?> s -> {
                response.setStatus(s.httpStatus());
                if (s.value() != null) {
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), s.value());
                }
            }
            case Result.Failure<?> f -> {
                response.setStatus(f.httpStatus());
                response.setContentType("application/json");
                objectMapper.writeValue(response.getWriter(), Map.of("error", f.error()));
            }
        }
    }
}
