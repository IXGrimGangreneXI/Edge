package org.asf.edge.common.webservices.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Makes it that the function requires a save container in the token<br/>
 * <br/>
 * Use a {@link org.asf.edge.common.services.accounts.AccountSaveContainer
 * AccountSaveContainer} parameter to receive the save container.
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface EdgeTokenRequireSave {
}
