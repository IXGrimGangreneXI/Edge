package org.asf.edge.common.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class LockingDriver implements Driver {

	private static boolean registered;
	private static LockingDriver driver;

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return url.startsWith("jdbc:locking:");
	}

	@Override
	public Connection connect(String url, Properties props) throws SQLException {
		if (!acceptsURL(url))
			return null;

		// Get requested driver URL
		url = url.substring("jdbc:locking:".length());
		while (url.startsWith("/"))
			url = url.substring(1);
		url = "jdbc:" + url;

		// Find driver
		Connection conn = DriverManager.getConnection(url);
		return new LockingConnection(conn);
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override
	public boolean jdbcCompliant() {
		return true;
	}

	public static synchronized Driver load() {
		if (!registered) {
			registered = true;
			driver = new LockingDriver();
			try {
				DriverManager.registerDriver(driver);
			} catch (SQLException throwables) {
				throwables.printStackTrace();
			}
		}
		return driver;
	}

	static {
		load();
	}
}
