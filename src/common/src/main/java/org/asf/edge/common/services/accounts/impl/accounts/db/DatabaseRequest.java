package org.asf.edge.common.services.accounts.impl.accounts.db;

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

	public abstract PreparedStatement prepareStatement(String query) throws SQLException;

	public void setDataObject(int i, String obj, PreparedStatement st) throws SQLException {
		st.setString(i, obj);
	}

	public abstract void close() throws SQLException;

}
