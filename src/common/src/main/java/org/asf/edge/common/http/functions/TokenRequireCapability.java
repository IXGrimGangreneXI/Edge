package org.asf.edge.common.http.functions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Defines required capabilities for token authentication
 * 
 * @author Sky Swimmer
 *
 */
@Repeatable(TokenRequireCapabilities.class)
@Retention(RUNTIME)
@Target(METHOD)
public @interface TokenRequireCapability {

	public String value();

}
