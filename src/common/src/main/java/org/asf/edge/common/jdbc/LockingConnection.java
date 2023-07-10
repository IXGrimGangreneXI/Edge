package org.asf.edge.common.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;

public class LockingConnection implements Connection {

	private Connection target;
	private static Connection lastConn;
	private static StackTraceElement[] lastStack;
	private static Object lockObj = new Object();

	public LockingConnection(Connection target) {
		this.target = target;
		boolean locked = true;
		synchronized (lockObj) {
			if (lastConn == null) {
				locked = false;
				lastStack = Thread.currentThread().getStackTrace();
				lastConn = target;
			}
		}

		// Wait for connection to free
		if (locked) {
			long start = System.currentTimeMillis();
			boolean warned = false;
			while (true) {
				if ((System.currentTimeMillis() - start) >= 10000) {
					if (!warned) {
						warned = true;
						String stackTr = "";
						synchronized (lockObj) {
							if (lastStack != null) {
								for (StackTraceElement ele : lastStack) {
									stackTr += "\n    at " + ele;
								}
								LogManager.getLogger("DATABASE").warn(
										"Database deadlock detected! Last connection did not close, its been 10 seconds since a second connection was attempted! Last connection stack trace:"
												+ stackTr);
							}
						}
					}
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
				if (lastConn == null) {
					synchronized (lockObj) {
						if (lastConn != null)
							continue;
						lastStack = Thread.currentThread().getStackTrace();
						lastConn = target;
						break;
					}
				}
			}
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		return target.isWrapperFor(arg0);
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		return target.unwrap(arg0);
	}

	@Override
	public void abort(Executor arg0) throws SQLException {
		target.abort(arg0);
	}

	@Override
	public void clearWarnings() throws SQLException {
		target.clearWarnings();
	}

	@Override
	public void close() throws SQLException {
		target.close();
		lastStack = null;
		lastConn = null;
	}

	@Override
	public void commit() throws SQLException {
		target.commit();
	}

	@Override
	public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
		return target.createArrayOf(arg0, arg1);
	}

	@Override
	public Blob createBlob() throws SQLException {
		return target.createBlob();
	}

	@Override
	public Clob createClob() throws SQLException {
		return target.createClob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return target.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return target.createSQLXML();
	}

	@Override
	public Statement createStatement() throws SQLException {
		return target.createStatement();
	}

	@Override
	public Statement createStatement(int arg0, int arg1) throws SQLException {
		return target.createStatement(arg0, arg1);
	}

	@Override
	public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
		return target.createStatement(arg0, arg1, arg2);
	}

	@Override
	public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
		return target.createStruct(arg0, arg1);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return target.getAutoCommit();
	}

	@Override
	public String getCatalog() throws SQLException {
		return target.getCatalog();
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return target.getClientInfo();
	}

	@Override
	public String getClientInfo(String arg0) throws SQLException {
		return target.getClientInfo(arg0);
	}

	@Override
	public int getHoldability() throws SQLException {
		return target.getHoldability();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return target.getMetaData();
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return target.getNetworkTimeout();
	}

	@Override
	public String getSchema() throws SQLException {
		return target.getSchema();
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return target.getTransactionIsolation();
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return target.getTypeMap();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return target.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return target.isClosed();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return target.isReadOnly();
	}

	@Override
	public boolean isValid(int arg0) throws SQLException {
		return target.isValid(arg0);
	}

	@Override
	public String nativeSQL(String arg0) throws SQLException {
		return target.nativeSQL(arg0);
	}

	@Override
	public CallableStatement prepareCall(String arg0) throws SQLException {
		return target.prepareCall(arg0);
	}

	@Override
	public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
		return target.prepareCall(arg0, arg1, arg2);
	}

	@Override
	public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
		return target.prepareCall(arg0, arg1, arg2, arg3);
	}

	@Override
	public PreparedStatement prepareStatement(String arg0) throws SQLException {
		return target.prepareStatement(arg0);
	}

	@Override
	public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
		return target.prepareStatement(arg0, arg1);
	}

	@Override
	public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
		return target.prepareStatement(arg0, arg1);
	}

	@Override
	public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
		return target.prepareStatement(arg0, arg1);
	}

	@Override
	public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
		return target.prepareStatement(arg0, arg1, arg2);
	}

	@Override
	public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
		return target.prepareStatement(arg0, arg2, arg3);
	}

	@Override
	public void releaseSavepoint(Savepoint arg0) throws SQLException {
		target.releaseSavepoint(arg0);
	}

	@Override
	public void rollback() throws SQLException {
		target.rollback();
	}

	@Override
	public void rollback(Savepoint arg0) throws SQLException {
		target.rollback(arg0);
	}

	@Override
	public void setAutoCommit(boolean arg0) throws SQLException {
		target.setAutoCommit(arg0);
	}

	@Override
	public void setCatalog(String arg0) throws SQLException {
		target.setCatalog(arg0);
	}

	@Override
	public void setClientInfo(Properties arg0) throws SQLClientInfoException {
		target.setClientInfo(arg0);
	}

	@Override
	public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException {
		target.setClientInfo(arg0, arg1);
	}

	@Override
	public void setHoldability(int arg0) throws SQLException {
		target.setHoldability(arg0);
	}

	@Override
	public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
		target.setNetworkTimeout(arg0, arg1);
	}

	@Override
	public void setReadOnly(boolean arg0) throws SQLException {
		target.setReadOnly(arg0);
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return target.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String arg0) throws SQLException {
		return target.setSavepoint(arg0);
	}

	@Override
	public void setSchema(String arg0) throws SQLException {
		target.setSchema(arg0);
	}

	@Override
	public void setTransactionIsolation(int arg0) throws SQLException {
		target.setTransactionIsolation(arg0);
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
		target.setTypeMap(arg0);
	}

}
