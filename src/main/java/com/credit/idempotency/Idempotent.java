package com.credit.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    // 2. Этой аннотацией помечаем endpoint, для которого повторный запрос с тем же ключом не должен выполняться заново.
}
