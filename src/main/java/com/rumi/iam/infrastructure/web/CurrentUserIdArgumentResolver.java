package com.rumi.iam.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Resolves {@link CurrentUserId} parameters from the {@code X-User-Id} header.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public UUID resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(CurrentUserId.HEADER);
        if (header == null || header.isBlank()) {
            throw new MissingUserIdException();
        }
        try {
            UUID userId = UUID.fromString(header.trim());
            // UUID.fromString accepts shortened forms such as "1-2-3-4-5"; only the canonical form is a real id.
            if (!userId.toString().equalsIgnoreCase(header.trim())) {
                throw new MissingUserIdException();
            }
            return userId;
        } catch (IllegalArgumentException malformed) {
            throw new MissingUserIdException();
        }
    }
}
