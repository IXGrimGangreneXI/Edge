package org.asf.edge.common.http.apihandlerutils.functions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Marks functions as token-secured with SoD-style security.<br/>
 * <br/>
 * Use a {@link org.asf.edge.common.tokens.SessionToken SessionToken} parameter
 * to receive the session token and a
 * {@link org.asf.edge.common.services.accounts.AccountObject AccountObject}
 * parameter to receive the account.
 * 
 * @author Sky Swimmer
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SodTokenSecured {
}
