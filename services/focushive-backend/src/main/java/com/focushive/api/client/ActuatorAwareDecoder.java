package com.focushive.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Custom Feign decoder that handles Spring Boot Actuator's vendor media types
 * in addition to standard JSON responses.
 *
 * This decoder is necessary because Actuator returns content with types like:
 * - application/vnd.spring-boot.actuator.v3+json
 * - application/vnd.spring-boot.actuator.v2+json
 *
 * The standard decoder doesn't recognize these as JSON, causing deserialization failures.
 */
public class ActuatorAwareDecoder implements Decoder {

    private final SpringDecoder delegate;

    public ActuatorAwareDecoder(ObjectMapper objectMapper) {
        // Create a custom message converter that handles Actuator media types
        MappingJackson2HttpMessageConverter actuatorConverter = new MappingJackson2HttpMessageConverter(objectMapper) {
            @Override
            public boolean canRead(Type type, Class<?> contextClass, org.springframework.http.MediaType mediaType) {
                // Accept any Actuator media type
                if (mediaType != null && mediaType.toString().contains("actuator")) {
                    return true;
                }
                return super.canRead(type, contextClass, mediaType);
            }

            @Override
            protected boolean canRead(org.springframework.http.MediaType mediaType) {
                // Accept any Actuator media type
                if (mediaType != null && mediaType.toString().contains("actuator")) {
                    return true;
                }
                return super.canRead(mediaType);
            }

            @Override
            public void setSupportedMediaTypes(java.util.List<org.springframework.http.MediaType> supportedMediaTypes) {
                // Add Actuator media types to supported types
                java.util.List<org.springframework.http.MediaType> types = new ArrayList<>(supportedMediaTypes);
                types.add(MediaType.valueOf("application/vnd.spring-boot.actuator.v3+json"));
                types.add(MediaType.valueOf("application/vnd.spring-boot.actuator.v2+json"));
                types.add(MediaType.valueOf("application/vnd.spring-boot.actuator+json"));
                super.setSupportedMediaTypes(types);
            }
        };

        // Configure the converter to handle Actuator media types
        ArrayList<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.valueOf("application/vnd.spring-boot.actuator.v3+json"));
        mediaTypes.add(MediaType.valueOf("application/vnd.spring-boot.actuator.v2+json"));
        mediaTypes.add(MediaType.valueOf("application/vnd.spring-boot.actuator+json"));
        actuatorConverter.setSupportedMediaTypes(mediaTypes);

        // Create HttpMessageConverters with our custom converter
        HttpMessageConverters converters = new HttpMessageConverters(actuatorConverter);

        // Create ObjectFactory for the converters
        ObjectFactory<HttpMessageConverters> messageConverters = () -> converters;

        // Create the delegate decoder with our custom converters
        this.delegate = new SpringDecoder(messageConverters);
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        // Check if this is an Actuator response by examining the Content-Type header
        Collection<String> contentTypes = response.headers().get("Content-Type");
        if (contentTypes != null) {
            for (String contentType : contentTypes) {
                if (contentType.contains("actuator")) {
                    // Log for debugging
                    if (response.body() != null) {
                        java.util.logging.Logger.getLogger(getClass().getName())
                            .fine("Decoding Actuator response with content type: " + contentType);
                    }
                    break;
                }
            }
        }

        // Delegate to the SpringDecoder which now handles Actuator types
        return delegate.decode(response, type);
    }
}