package com.airepublic.bmstoinverter.core.protocol.rs485;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import jakarta.inject.Qualifier;

/**
 * Qualifier to identify a {@link Port} that handles RS485 messages.
 */
@Qualifier
@Retention(RUNTIME)
@Documented
public @interface RS485 {

}
