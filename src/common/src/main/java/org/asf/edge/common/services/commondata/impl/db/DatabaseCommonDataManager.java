package org.asf.edge.common.services.commondata.impl.db;

import java.sql.SQLException;

import org.asf.edge.common.services.commondata.CommonKvDataContainer;
import org.asf.edge.common.services.tabledata.TableRow;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.commondata.CommonDataTableContainer;

public abstract class DatabaseCommonDataManager extends CommonDataManager {

	@Override
	protected CommonKvDataContainer getKeyValueContainerInternal(String rootNodeName) {
		return new DatabaseCommonKvDataContainer(this, "CDC2_" + rootNodeName);
	}

	@Override
	protected <T extends TableRow> CommonDataTableContainer<T> getDataTableContainerInternal(String tableName, Class<T> cls) {
		return new DatabaseCommonDataTableContainer<T>(cls, "CTC2_" + tableName, this);
	}

	/**
	 * Called to create database requests
	 * 
	 * @return DatabaseRequest instance
	 */
	public abstract DatabaseRequest createRequest() throws SQLException;

}
