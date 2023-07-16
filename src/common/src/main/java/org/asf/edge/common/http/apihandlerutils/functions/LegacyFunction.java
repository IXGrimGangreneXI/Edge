package org.asf.edge.common.http.apihandlerutils.functions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Marks methods as functions, if annotation has a value, it is used as function
 * name, else the method name is used.<br/>
 * <br/>
 * <b>Note</b>: the annotated method must contain a {@link FunctionInfo}
 * parameter, no other parameters are supported.
 * 
 * @deprecated Use {@link Function} instead with the updated API format
 * @author Sky Swimmer
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
@Deprecated
public @interface LegacyFunction {
	public String value() default ("<auto>");

	public String[] allowedMethods() default { "GET", "POST" };
}
