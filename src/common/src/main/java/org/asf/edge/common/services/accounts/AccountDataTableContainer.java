package org.asf.edge.common.services.accounts;

import org.asf.edge.common.services.tabledata.DataTable;
import org.asf.edge.common.services.tabledata.TableRow;

/**
 *
 * Account data table container
 *
 * @author Sky Swimmer
 *
 */
public abstract class AccountDataTableContainer<T extends TableRow> extends DataTable<T> {

	protected AccountDataTableContainer(Class<T> cls) {
		super(cls);
	}

	/**
	 * Retrieves the account object associated with this data container
	 *
	 * @return AccountObject instance
	 */
	public abstract AccountObject getAccount();

	/**
	 * Retrieves the save container associated with this data container, returns
	 * null if this is an account-wide container
	 *
	 * @return AccountSaveContainer instance or null
	 */
	public abstract AccountSaveContainer getSave();

}
