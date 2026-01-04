package com.mootann.arxivdaily.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    String value();

    String[] roles() default {"ADMIN"};
}
