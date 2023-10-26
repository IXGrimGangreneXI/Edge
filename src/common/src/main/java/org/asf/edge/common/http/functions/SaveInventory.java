package org.asf.edge.common.http.functions;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Assigns the save inventory to the parameter value
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface SaveInventory {
}
