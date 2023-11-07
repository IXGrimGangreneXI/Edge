package org.asf.edge.common.entities.tables.items;

import org.asf.edge.common.services.tabledata.TableRow;
import org.asf.edge.common.services.tabledata.annotations.ForceUseFilterFields;
import org.asf.edge.common.services.tabledata.annotations.TableColumn;
import org.asf.edge.common.services.tabledata.annotations.UseAsFilter;

@ForceUseFilterFields
public class PopularItemRow extends TableRow {

	@TableColumn
	@UseAsFilter
	public int itemID;

	@TableColumn
	public int popularity;

}
