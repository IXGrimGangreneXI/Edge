package org.asf.edge.common.services.commondata.impl.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 
 * Abstract class holding methods used to call database access
 * 
 * @author Sky Swimmer
 *
 */
public abstract class DatabaseRequest {

	public abstract PreparedStatement createPreparedStatement(String query) throws SQLException;

	public void setDataObject(int i, String obj, PreparedStatement st) throws SQLException {
		st.setString(i, obj);
	}

	public abstract void finish() throws SQLException;

}
