package org.asf.edge.common.services.commondata;

import org.asf.nexus.tables.DataTable;
import org.asf.nexus.tables.TableRow;

/**
 * 
 * Common data table container
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class CommonDataTableContainer<T extends TableRow> extends DataTable<T> {

	protected CommonDataTableContainer(Class<T> cls) {
		super(cls);
	}

}
