package org.asf.edge.common.http.apihandlerutils.functions;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Marks the parameter as an encrypted parameter
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface SodEncryptedParam {
}
