package com.airepublic.bmstoinverter.core;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The type of {@link Port} to use with the specified {@link Protocol}, e.g. {@link Protocol.CAN},
 * {@link Protocol.RS485} or {@link Protocol.MODBUS},
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface PortType {
    Protocol value();
}
