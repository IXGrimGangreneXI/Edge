package org.asf.edge.common.webservices.annotations;

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
@Repeatable(EdgeTokenRequireCapabilities.class)
@Retention(RUNTIME)
@Target(METHOD)
public @interface EdgeTokenRequireCapability {

	public String value();

}
