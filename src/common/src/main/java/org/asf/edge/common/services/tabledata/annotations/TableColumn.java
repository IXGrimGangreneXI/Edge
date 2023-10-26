package org.asf.edge.common.services.tabledata.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Marks a field as table column for the data table system
 * 
 * @author Sky Swimmer
 * 
 */
@Retention(RUNTIME)
@Target({ FIELD })
public @interface TableColumn {
}
