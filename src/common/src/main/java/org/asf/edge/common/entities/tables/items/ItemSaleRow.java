package org.asf.edge.common.entities.tables.items;

import java.util.Date;

import org.asf.nexus.tables.TableRow;
import org.asf.nexus.tables.annotations.ForceUseFilterFields;
import org.asf.nexus.tables.annotations.TableColumn;
import org.asf.nexus.tables.annotations.UseAsFilter;

@ForceUseFilterFields
public class ItemSaleRow extends TableRow {

	@TableColumn
	@UseAsFilter
	public int categoryID;

	@TableColumn
	public Date startTime;

	@TableColumn
	public Date endTime;

	@TableColumn
	public float modifier;

	@TableColumn
	public int[] itemIDs;

	@TableColumn
	public boolean memberOnly;

}
