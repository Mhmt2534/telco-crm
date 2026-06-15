package com.telcocrm.common.web.resolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the current request's {@link com.telcocrm.common.core.context.UserContext} (populated
 * from the gateway-injected headers) into a controller method parameter.
 *
 * <pre>{@code @GetMapping("/me") public X me(@CurrentUser UserContext user) { ... } }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
