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
 * <b>Note</b>: the annotated method must return a {@link FunctionResult} object
 * and its parameters must contain a {@link FunctionInfo} parameter, no other
 * parameters are supported.
 * 
 * @author Sky Swimmer
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Function {
	public boolean allowSubPaths() default(false);
	
	public String value() default ("<auto>");

	public String[] allowedMethods() default { "GET", "POST" };
}
