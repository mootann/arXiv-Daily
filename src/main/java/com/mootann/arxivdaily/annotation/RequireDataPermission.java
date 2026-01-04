package com.mootann.arxivdaily.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireDataPermission {

    DataPermissionType type() default DataPermissionType.OWN;

    String orgTagParam() default "";

    enum DataPermissionType {
        OWN,
        ORG,
        ALL
    }
}
