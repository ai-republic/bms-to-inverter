package com.airepublic.bmstoinverter.core.protocol.modbus;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Qualifier to identify a {@link Port} that handles ModBus messages.
 */
@Qualifier
@Retention(RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
public @interface ModBus {

}
