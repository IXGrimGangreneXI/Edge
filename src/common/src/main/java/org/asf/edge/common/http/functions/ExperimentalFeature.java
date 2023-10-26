package org.asf.edge.common.http.functions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Marks methods as experimental, making them require specific experimental
 * feature flags on the server
 * 
 * @author Sky Swimmer
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface ExperimentalFeature {

	/**
	 * Defines the experimental feature thats required
	 * 
	 * @return Experimental feature key
	 */
	public String value();

	/**
	 * Marks this method as a reverse feature, meaning this method only is enabled
	 * should the feature be disabled.
	 * 
	 * @return True to enable reverse-mode, false otherwise
	 */
	public boolean isReverse() default (false);

}
