package org.asf.edge.common.entities.tables.avatar;

import org.asf.edge.common.services.tabledata.TableRow;
import org.asf.edge.common.services.tabledata.annotations.TableColumn;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AvatarObjectContainer extends TableRow {

	@TableColumn
	public ObjectNode avatar;

}
