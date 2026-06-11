package com.docflow.ai.common.idempotent;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    String prefix() default "idempotent";
    int expireSeconds() default 10;
    String message() default "Duplicate request detected";
}
