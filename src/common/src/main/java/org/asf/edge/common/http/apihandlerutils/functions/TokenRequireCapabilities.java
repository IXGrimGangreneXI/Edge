package org.asf.edge.common.http.apihandlerutils.functions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Defines required capabilities for token authentication
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface TokenRequireCapabilities {

	public TokenRequireCapability[] value();

}
