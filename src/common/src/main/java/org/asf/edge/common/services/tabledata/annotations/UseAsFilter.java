package org.asf.edge.common.services.tabledata.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Marks a field as a filter field, this field will be used to filter assignment
 * requests if there are no previous values in cache
 * 
 * @author Sky Swimmer
 * 
 */
@Retention(RUNTIME)
@Target({ FIELD })
public @interface UseAsFilter {
}
