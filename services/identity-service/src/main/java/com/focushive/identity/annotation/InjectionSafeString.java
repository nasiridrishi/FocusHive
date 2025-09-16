package com.focushive.identity.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.focushive.identity.config.InjectionSafeStringDeserializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark string fields that should use injection-safe deserialization.
 * This prevents NoSQL injection attempts by rejecting non-string JSON tokens.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = InjectionSafeStringDeserializer.class)
public @interface InjectionSafeString {
}