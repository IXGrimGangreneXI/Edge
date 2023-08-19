package org.asf.edge.common.http.apihandlerutils.functions;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Marks the parameter as a request form parameter
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface SodRequestParam {

	/**
	 * Defines the payload entry name to retrieve, by default it will use the
	 * parameter name
	 */
	public String value() default "";

}
