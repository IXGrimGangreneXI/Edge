package org.asf.edge.common.webservices.annotations;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Assigns the specified key/value data container to the parameter value, uses
 * save key/value data if present, otherwise it uses the account key/value
 * container
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface KvContainer {

	/**
	 * Key/value container name
	 */
	public String value();

}
