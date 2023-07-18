package org.asf.edge.common.services.commondata.impl.db;

import java.sql.SQLException;

import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;

public abstract class DatabaseCommonDataManager extends CommonDataManager {

	@Override
	protected CommonDataContainer getContainerInternal(String rootNodeName) {
		return new DatabaseCommonDataContainer(this, "CDC2_" + rootNodeName);
	}

	/**
	 * Called to create database requests
	 * 
	 * @return DatabaseRequest instance
	 */
	public abstract DatabaseRequest createRequest() throws SQLException;

}
