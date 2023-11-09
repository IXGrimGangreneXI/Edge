package org.asf.edge.common.entities.tables.avatar;

import org.asf.nexus.tables.TableRow;
import org.asf.nexus.tables.annotations.TableColumn;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AvatarObjectContainer extends TableRow {

	@TableColumn
	public ObjectNode avatar;

}
