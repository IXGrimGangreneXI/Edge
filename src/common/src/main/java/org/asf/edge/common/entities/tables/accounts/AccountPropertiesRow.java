package org.asf.edge.common.entities.tables.accounts;

import org.asf.nexus.tables.TableRow;
import org.asf.nexus.tables.annotations.TableColumn;

public class AccountPropertiesRow extends TableRow {

	@TableColumn
	public String emailAddress;

	@TableColumn
	public String accountUsername;

	@TableColumn
	public long registrationTimestamp;

	@TableColumn
	public long lastLoginTime;

	@TableColumn
	public boolean isGuestAccount;

	@TableColumn
	public boolean isMultiplayerEnabled;

	@TableColumn
	public boolean isChatEnabled;

	@TableColumn
	public boolean isStrictChatFilterEnabled;

	@TableColumn
	public String currentServerClusterID;

	@TableColumn
	public boolean isOnCluster;

}
