package org.asf.edge.common.http.functions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Marks methods as SoD requests.<br/>
 * <br/>
 * Use a {@link FunctionInfo} parameter to receive the function info object and
 * use a {@link org.asf.edge.common.http.ServiceRequestInfo ServiceRequestInfo}
 * parameter to receive the request info. <br/>
 * <br/>
 * Use {@link SodRequestParam} on parameters to load from the request payload.
 * 
 * @author Sky Swimmer
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SodRequest {
}
