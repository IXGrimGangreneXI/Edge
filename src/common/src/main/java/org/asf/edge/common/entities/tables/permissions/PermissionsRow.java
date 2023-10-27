package org.asf.edge.common.entities.tables.permissions;

import java.util.ArrayList;

import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.tabledata.TableRow;
import org.asf.edge.common.services.tabledata.annotations.TableColumn;

public class PermissionsRow extends TableRow {

	@TableColumn
	public PermissionLevel level = PermissionLevel.PLAYER;

	@TableColumn
	public ArrayList<String> allowedPermissions = new ArrayList<String>();

	@TableColumn
	public ArrayList<String> deniedPermissions = new ArrayList<String>();

}
