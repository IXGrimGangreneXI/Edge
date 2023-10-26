package org.asf.edge.common.services.commondata;

import org.asf.edge.common.services.tabledata.DataTable;
import org.asf.edge.common.services.tabledata.TableRow;

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
