package org.asf.edge.common.services.commondata.impl.db;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.CommonDataTableContainer;
import org.asf.edge.common.services.tabledata.DataEntry;
import org.asf.edge.common.services.tabledata.DataFilter;
import org.asf.edge.common.services.tabledata.DataSet;
import org.asf.edge.common.services.tabledata.DataTable;
import org.asf.edge.common.services.tabledata.DataType;
import org.asf.edge.common.services.tabledata.TableRow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatabaseCommonDataTableContainer<T extends TableRow> extends CommonDataTableContainer<T> {

	private String table;
	private DatabaseCommonDataManager mgr;
	private Logger logger = LogManager.getLogger("CommonDataManager");

	private static ObjectMapper mapper = new ObjectMapper();

	protected DatabaseCommonDataTableContainer(Class<T> cls, String table, DatabaseCommonDataManager manager) {
		super(cls);
		this.table = table;
		this.mgr = manager;
	}

	@Override
	protected boolean hasRowsInternal(DataFilter dataFilter) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Prepare query
				String query = "SELECT " + getLayout().getColumns()[0].columnType + "_"
						+ getLayout().getColumnNames()[0] + " FROM " + table;
				boolean first = true;
				for (String column : dataFilter.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (dataFilter.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (first)
						query += " WHERE ";
					else
						query += " AND ";
					query += layout.columnType + "_" + column.toUpperCase() + " = ?";
					first = false;
				}

				// Create prepared statement
				var statement = req.createPreparedStatement(query);

				// Populate statement
				populateStatement(statement, dataFilter, 1, req);

				// Check result
				ResultSet res = statement.executeQuery();
				boolean r = res.next();
				res.close();
				statement.close();
				return r;
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check if table '" + table
					+ "' has rows matching the query", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected DataSet getFirstRowInternal(DataFilter dataFilter, String... columnNames) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Prepare request string
				boolean first = true;
				String reqStr = "";
				for (String column : columnNames) {
					if (!first)
						reqStr += ",";
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					reqStr += layout.columnType + "_" + column.toUpperCase();
					first = false;
				}
				if (first)
					throw new IOException(
							"Cannot create a empty table data request, please request at least one column!");

				// Prepare query
				String query = "SELECT " + reqStr + " FROM " + table;
				first = true;
				for (String column : dataFilter.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (dataFilter.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (first)
						query += " WHERE ";
					else
						query += " AND ";
					query += layout.columnType + "_" + column.toUpperCase() + " = ?";
					first = false;
				}

				// Create prepared statement
				var statement = req.createPreparedStatement(query);

				// Populate statement
				populateStatement(statement, dataFilter, 1, req);

				// Check result
				ResultSet res = statement.executeQuery();
				DataSet r = null;
				if (res.next()) {
					// Populate result set
					r = new DataSet();
					populateResultDataSet(r, res, columnNames);
				}
				res.close();
				statement.close();
				return r;
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying retrieve rows from table '" + table
					+ "' matching the query", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected DataSet[] getAllRowsInternal(DataFilter dataFilter, String... columnNames) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Prepare request string
				boolean first = true;
				String reqStr = "";
				for (String column : columnNames) {
					if (!first)
						reqStr += ",";
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					reqStr += layout.columnType + "_" + column.toUpperCase();
					first = false;
				}
				if (first)
					throw new IOException(
							"Cannot create a empty table data request, please request at least one column!");

				// Prepare query
				String query = "SELECT " + reqStr + " FROM " + table;
				first = true;
				for (String column : dataFilter.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (dataFilter.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (first)
						query += " WHERE ";
					else
						query += " AND ";
					query += layout.columnType + "_" + column.toUpperCase() + " = ?";
					first = false;
				}

				// Create prepared statement
				var statement = req.createPreparedStatement(query);

				// Populate statement
				populateStatement(statement, dataFilter, 1, req);

				// Read result
				ResultSet res = statement.executeQuery();
				ArrayList<DataSet> r = new ArrayList<DataSet>();
				while (res.next()) {
					// Populate set
					DataSet rS = new DataSet();
					populateResultDataSet(rS, res, columnNames);
					r.add(rS);
				}
				res.close();
				statement.close();
				return r.toArray(t -> new DataSet[t]);
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying retrieve rows from table '" + table
					+ "' matching the query", e);
			throw new IOException("SQL error", e);
		}
	}

	private void createRow(DataSet set) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Prepare request string
				boolean first = true;
				String colStr = "";
				String valStr = "";
				for (String column : set.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (set.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (!first) {
						colStr += ", ";
						valStr += ", ";
					}
					colStr += layout.columnType + "_" + column.toUpperCase();
					valStr += "?";
					first = false;
				}
				if (first)
					throw new IOException(
							"Cannot create a empty table assignment request, please assign at least one column!");

				// Prepare query
				String query = "INSERT INTO " + table + " (" + colStr + ") VALUES (" + valStr + ")";

				// Create prepared statement
				var statement = req.createPreparedStatement(query);

				// Populate statement
				populateStatement(statement, set, 1, req);

				// Read result
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to create new row in table '" + table + "'",
					e);
			throw new IOException("SQL error", e);
		}
	}

	private void setRow(DataFilter dataFilter, DataSet set) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Prepare request string
				boolean first = true;
				String reqStr = "";
				int offset = 1;
				for (String column : set.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (set.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (!first) {
						reqStr += ", ";
					}
					reqStr += layout.columnType + "_" + column.toUpperCase() + " = ?";
					first = false;
					offset++;
				}
				if (first)
					throw new IOException(
							"Cannot create a empty table assignment request, please assign at least one column!");

				// Build query
				String qStr = "";
				first = true;
				for (String column : dataFilter.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (dataFilter.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (!first) {
						qStr += " AND ";
					} else
						qStr = " WHERE ";
					qStr += layout.columnType + "_" + column.toUpperCase() + " = ?";
					first = false;
				}

				// Prepare query
				String query = "UPDATE " + table + " SET " + reqStr + qStr;

				// Create prepared statement
				var statement = req.createPreparedStatement(query);

				// Populate statement
				populateStatement(statement, set, 1, req);
				populateStatement(statement, dataFilter, offset, req);

				// Read result
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to assign existing rows in table '"
					+ table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void setRowsInternal(DataFilter dataFilter, DataSet set) throws IOException {
		// Check
		if (!hasRowsInternal(dataFilter))
			createRow(set);
		else
			setRow(dataFilter, set);
	}

	@Override
	protected void removeRowsInternal(DataFilter dataFilter) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Build query
				String qStr = "";
				boolean first = true;
				for (String column : dataFilter.getColumnNames()) {
					// Check null
					DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
					if (dataFilter.getValueType(column) == DataType.NULL) {
						// Check type
						DataType columnType = layout.columnType;
						if (columnType != DataType.OBJECT && columnType != DataType.STRING)
							continue; // Cannot check null
					}
					if (!first) {
						qStr += " AND ";
					} else
						qStr = " WHERE ";
					qStr += layout.columnType + "_" + column.toUpperCase() + " = ?";
					first = false;
				}

				// Prepare query
				String query = "DELETE FROM " + table + qStr;

				// Create prepared statement
				var statement = req.createPreparedStatement(query);

				// Populate statement
				populateStatement(statement, dataFilter, 1, req);

				// Read result
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to remove rows from table '" + table
					+ "' matching the query", e);
			throw new IOException("SQL error", e);
		}
	}

	private void populateResultDataSet(DataSet set, ResultSet res, String[] requestedColumns)
			throws SQLException, JsonMappingException, JsonProcessingException {
		// Go through columns
		for (String column : requestedColumns) {
			// Retrieve the type
			DataType type = getLayout().getLayout(column).columnType;

			// Handle type
			switch (type) {

			// Null values arent used by columnType
			case NULL:
				// Unused
				break;

			// Boolean value
			case BOOLEAN:
				set.setValue(column,
						res.getBoolean(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Date value
			case DATE:
				set.setValue(column,
						new Date(res.getLong(getLayout().getLayout(column).columnType + "_" + column.toUpperCase())));
				break;

			// Byte value
			case BYTE:
				set.setValue(column,
						res.getByte(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Byte array value
			case BYTE_ARRAY:
				set.setValue(column,
						res.getBytes(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Char value
			case CHAR: {
				String val = res.getString(getLayout().getLayout(column).columnType + "_" + column.toUpperCase());
				if (val != null)
					set.setValue(column, val.charAt(0));
				break;
			}

			// Double value
			case DOUBLE:
				set.setValue(column,
						res.getDouble(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Float value
			case FLOAT:
				set.setValue(column,
						res.getFloat(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Int value
			case INT:
				set.setValue(column, res.getInt(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Long value
			case LONG:
				set.setValue(column,
						res.getLong(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// Object value
			case OBJECT:
				DataTable.DataTableLayout.EntryLayout layout = getLayout().getLayout(column);
				String val = res.getString(layout.columnType + "_" + column.toUpperCase());
				if (val != null)
					set.setValue(column, mapper.readValue(val, layout.objectType));
				else
					set.setValue(column, null);
				break;

			// Short value
			case SHORT:
				set.setValue(column,
						res.getShort(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			// String value
			case STRING:
				set.setValue(column,
						res.getString(getLayout().getLayout(column).columnType + "_" + column.toUpperCase()));
				break;

			}
		}
	}

	private void populateStatement(PreparedStatement statement, DataSet set, int i, DatabaseRequest req)
			throws SQLException, IOException {
		for (DataEntry column : set) {
			// Handle type
			switch (column.getValueType()) {

			// Null type
			case NULL: {
				// Check support for null values
				DataType columnType = getLayout().getLayout(column.getColumnName()).columnType;
				if (columnType != DataType.OBJECT && columnType != DataType.STRING)
					continue; // Cannot use null

				// Add to statement
				if (columnType != DataType.OBJECT)
					statement.setString(i++, null);
				else
					req.setDataObject(i++, null, statement);
				break;
			}

			// Boolean type
			case BOOLEAN: {
				// Add to statement
				statement.setBoolean(i++, column.getValue(Boolean.class));
				break;
			}

			// Date type
			case DATE: {
				// Add to statement
				statement.setLong(i++, column.getValue(Date.class).getTime());
				break;
			}

			// Byte type
			case BYTE: {
				// Add to statement
				statement.setByte(i++, column.getValue(Byte.class));
				break;
			}

			// Byte array type
			case BYTE_ARRAY: {
				// Add to statement
				statement.setBytes(i++, column.getValue(byte[].class));
				break;
			}

			// Character type
			case CHAR: {
				// Add to statement
				statement.setString(i++, new String(new char[] { column.getValue(Character.class) }));
				break;
			}

			// Double type
			case DOUBLE: {
				// Add to statement
				statement.setDouble(i++, column.getValue(Double.class));
				break;
			}

			// Float type
			case FLOAT: {
				// Add to statement
				statement.setFloat(i++, column.getValue(Float.class));
				break;
			}

			// Int type
			case INT: {
				// Add to statement
				statement.setInt(i++, column.getValue(Integer.class));
				break;
			}

			// Long type
			case LONG: {
				// Add to statement
				statement.setLong(i++, column.getValue(Long.class));
				break;
			}

			// Object type
			case OBJECT: {
				// Add to statement
				req.setDataObject(i++, mapper.writeValueAsString(column.getValue(Object.class)), statement);
				break;
			}

			// Short type
			case SHORT: {
				// Add to statement
				statement.setShort(i++, column.getValue(Short.class));
				break;
			}

			// String type
			case STRING: {
				// Add to statement
				statement.setString(i++, column.getValue(String.class));
				break;
			}

			}
		}
	}

}
