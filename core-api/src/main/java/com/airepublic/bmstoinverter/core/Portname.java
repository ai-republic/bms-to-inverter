package com.airepublic.bmstoinverter.core;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be used on the {@link Port} member field to declare the portname.
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Portname {
    String value();
}
