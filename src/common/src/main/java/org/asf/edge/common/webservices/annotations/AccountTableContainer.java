package org.asf.edge.common.webservices.annotations;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.asf.nexus.tables.TableRow;

/**
 *
 * Assigns the specified account data table container to the parameter value
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface AccountTableContainer {

	/**
	 * Data table container name
	 */
	public String name();

	/**
	 * Row type
	 */
	public Class<? extends TableRow> rowType();

}
