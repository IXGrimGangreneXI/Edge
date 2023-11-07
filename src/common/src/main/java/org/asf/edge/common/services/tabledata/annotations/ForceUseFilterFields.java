package org.asf.edge.common.services.tabledata.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Makes the data table serialization system always use filter fields instead of
 * the value cache
 * 
 * @author Sky Swimmer
 * 
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ForceUseFilterFields {
}
