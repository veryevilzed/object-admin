package ru.ogres.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by zed on 17.08.16.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface ObjectAdminField {
    String name() default "";
    String description() default "";
    String type() default "";
    int index() default -1;
    boolean hidden() default false;
}
