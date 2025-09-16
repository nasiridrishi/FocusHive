package com.focushive.notification.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.annotation.SensitiveData;
import com.focushive.notification.security.DataEncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Interceptor for automatic encryption/decryption of sensitive data in requests and responses.
 * Works with @SensitiveData annotation to identify fields that need encryption.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionInterceptor implements HandlerInterceptor {

    private final DataEncryptionService encryptionService;

    @Value("${security.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${security.encryption.log-operations:false}")
    private boolean logOperations;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler) throws Exception {
        if (!encryptionEnabled) {
            return true;
        }

        // Add encryption header to indicate support
        response.setHeader("X-Encryption-Supported", "true");
        response.setHeader("X-Encryption-Algorithm", "AES-256-GCM");

        if (logOperations) {
            log.debug("Encryption interceptor processing request: {} {}",
                request.getMethod(), request.getRequestURI());
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                          Object handler, ModelAndView modelAndView) throws Exception {
        // No post-processing needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) throws Exception {
        if (logOperations && ex != null) {
            log.error("Request processing failed: {} {}", request.getMethod(),
                request.getRequestURI(), ex);
        }
    }
}

/**
 * Request body advice to decrypt sensitive data in incoming requests.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
class DecryptionRequestBodyAdvice implements RequestBodyAdvice {

    private final DataEncryptionService encryptionService;

    @Value("${security.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                          Class<? extends HttpMessageConverter<?>> converterType) {
        if (!encryptionEnabled) {
            return false;
        }

        // Check if the parameter or its class has sensitive fields
        Class<?> parameterType = methodParameter.getParameterType();
        return hasSensitiveFields(parameterType) ||
               methodParameter.hasParameterAnnotation(SensitiveData.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                          Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                               Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (body == null) {
            return body;
        }

        try {
            // Decrypt sensitive fields in the request body
            decryptSensitiveFields(body);
            log.debug("Decrypted sensitive fields in request body for: {}",
                parameter.getMethod().getName());
        } catch (Exception e) {
            log.error("Failed to decrypt request body", e);
            throw new RuntimeException("Failed to process encrypted request", e);
        }

        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                 Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    private void decryptSensitiveFields(Object obj) throws IllegalAccessException {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(SensitiveData.class)) {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (value instanceof String) {
                    String decrypted = encryptionService.decrypt((String) value);
                    field.set(obj, decrypted);
                } else if (value != null && !isPrimitiveOrWrapper(field.getType())) {
                    // Recursively decrypt nested objects
                    decryptSensitiveFields(value);
                }
            }
        }
    }

    private boolean hasSensitiveFields(Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive() || clazz.getPackage().getName().startsWith("java")) {
            return false;
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SensitiveData.class)) {
                return true;
            }
        }

        // Check parent class
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return hasSensitiveFields(superClass);
        }

        return false;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == Boolean.class || clazz == Byte.class ||
               clazz == Character.class || clazz == Short.class ||
               clazz == Integer.class || clazz == Long.class ||
               clazz == Float.class || clazz == Double.class ||
               clazz == String.class;
    }
}

/**
 * Response body advice to encrypt sensitive data in outgoing responses.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
class EncryptionResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final DataEncryptionService encryptionService;

    @Value("${security.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${security.encryption.response.enabled:true}")
    private boolean responseEncryptionEnabled;

    private static final Set<String> ENCRYPTED_PATHS = new HashSet<>(Arrays.asList(
        "/api/notifications",
        "/api/notifications/preferences",
        "/api/users",
        "/api/auth"
    ));

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!encryptionEnabled || !responseEncryptionEnabled) {
            return false;
        }

        // Check if the return type has sensitive fields
        Class<?> returnClass = returnType.getParameterType();
        return hasSensitiveFields(returnClass) ||
               returnType.hasMethodAnnotation(SensitiveData.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return body;
        }

        // Check if the path requires encryption
        String path = request.getURI().getPath();
        boolean shouldEncrypt = ENCRYPTED_PATHS.stream().anyMatch(path::startsWith);

        if (!shouldEncrypt && !returnType.hasMethodAnnotation(SensitiveData.class)) {
            return body;
        }

        try {
            // Encrypt sensitive fields in the response
            encryptSensitiveFields(body);

            // Add header to indicate response is encrypted
            response.getHeaders().add("X-Response-Encrypted", "true");

            log.debug("Encrypted sensitive fields in response for: {}",
                returnType.getMethod().getName());
        } catch (Exception e) {
            log.error("Failed to encrypt response body", e);
            // Return unencrypted in case of failure (fail open for availability)
            return body;
        }

        return body;
    }

    private void encryptSensitiveFields(Object obj) throws IllegalAccessException {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(SensitiveData.class)) {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (value instanceof String) {
                    String encrypted = encryptionService.encrypt((String) value);
                    field.set(obj, encrypted);

                    // Log masking if enabled
                    SensitiveData annotation = field.getAnnotation(SensitiveData.class);
                    if (annotation.maskInLogs()) {
                        log.debug("Encrypted field '{}' in {}", field.getName(), clazz.getSimpleName());
                    }
                } else if (value != null && !isPrimitiveOrWrapper(field.getType())) {
                    // Recursively encrypt nested objects
                    encryptSensitiveFields(value);
                }
            }
        }
    }

    private boolean hasSensitiveFields(Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive() || clazz.getPackage().getName().startsWith("java")) {
            return false;
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SensitiveData.class)) {
                return true;
            }
        }

        // Check parent class
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return hasSensitiveFields(superClass);
        }

        return false;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == Boolean.class || clazz == Byte.class ||
               clazz == Character.class || clazz == Short.class ||
               clazz == Integer.class || clazz == Long.class ||
               clazz == Float.class || clazz == Double.class ||
               clazz == String.class;
    }
}