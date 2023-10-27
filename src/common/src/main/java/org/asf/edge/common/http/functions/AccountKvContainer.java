package org.asf.edge.common.http.functions;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Assigns the specified account key/value data container to the parameter value
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface AccountKvContainer {

	/**
	 * Key/value container name
	 */
	public String value();

}
