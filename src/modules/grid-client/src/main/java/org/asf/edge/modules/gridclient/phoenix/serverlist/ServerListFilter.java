package org.asf.edge.modules.gridclient.phoenix.serverlist;

/**
 * 
 * Server list filtering utility
 * 
 * @author Sky Swimmer
 *
 */
public class ServerListFilter {

	private ServerListFilterType filterType;
	private String key;
	private String value;

	/**
	 * Creates a new filter parameter<br/>
	 * <br/>
	 * There are a few built-in filtering keys, <b>note that the built-in keys only
	 * work with <see cref="ServerListFilterType.DEFUALT"/> as filter type.</b><br/>
	 * Any non-built-in filter keys will check the Details Block of the server entry
	 * and do work with all filtering types.<br/>
	 * <br/>
	 * <b>Please note that filter types simply add operators to the key such as
	 * `!=`, `==`, `!~` and `=~`, prefixing keys with this will trigger the server
	 * to use associated filtering method.<br/>
	 * </b> <br/>
	 * <br/>
	 * Here follows a list of filtering keys built into the Phoenix API:<br/>
	 * - <u>id</u>: filters by server ID, this uses the
	 * <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>ownerId</u>: filters by the server's owner ID, this uses the
	 * <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>version</u>: filters by the server's game version, this uses the
	 * <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>protocol</u>: filters by the server's game protocol version, this uses
	 * the <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>phoenixProtocol</u>: filters by the server's phoenix protocol version,
	 * this uses the <see cref="ServerListFilterType.STRICT"/> filtering type on the
	 * server side<br/>
	 * - <u>address</u>: searches the server address list for the IP address
	 * specified in the <paramref name="filterString"/><br/>
	 * - <u>players</u>: processed using a more complex parser, the
	 * <paramref name="filterString"/> is used to verify the player count (see below
	 * for details)<br/>
	 * <br/>
	 * <br/>
	 * Player count filtering:<br/>
	 * Phoenix allows for filtering servers by player count, this uses special
	 * operators specified in the <paramref name="filterString"/><br/>
	 * Note that between the operator and value, <u>there must not be a space</u>
	 * else it is not parsed. Example: `<u>&lt;=30</u>` (below or equal 30
	 * players)<br/>
	 * <br/>
	 * Here follows the list of operators and their behaviour:<br/>
	 * - <u>notfull</u> (exact filter string): checks if the server is not full<br/>
	 * - <u>&lt;<b>value</b></u>: checks if the player count is below
	 * <b>value</b><br/>
	 * - <u>&lt;=<b>value</b></u>: checks if the player count is below or equal to
	 * <b>value</b><br/>
	 * - <u>&gt;<b>value</b></u>: checks if the player count is greater than
	 * <b>value</b><br/>
	 * - <u>&gt;=<b>value</b></u>: checks if the player count is greater or equal to
	 * <b>value</b><br/>
	 * - <u><b>value</b></u>: checksc if the player count is equal to <b>value</b>
	 * </summary>
	 * 
	 * @param type         Filter type
	 * @param key          Filter key
	 * @param filterString Filter string
	 */
	public ServerListFilter(ServerListFilterType type, String key, String filterString) {
		this.filterType = type;
		this.key = key;
		this.value = filterString;
	}

	/**
	 * Creates a new filter parameter<br/>
	 * <br/>
	 * There are a few built-in filtering keys, <b>note that the built-in keys only
	 * work with <see cref="ServerListFilterType.DEFUALT"/> as filter type.</b><br/>
	 * Any non-built-in filter keys will check the Details Block of the server entry
	 * and do work with all filtering types.<br/>
	 * <br/>
	 * <b>Please note that filter types simply add operators to the key such as
	 * `!=`, `==`, `!~` and `=~`, prefixing keys with this will trigger the server
	 * to use associated filtering method.<br/>
	 * </b> <br/>
	 * <br/>
	 * Here follows a list of filtering keys built into the Phoenix API:<br/>
	 * - <u>id</u>: filters by server ID, this uses the
	 * <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>ownerId</u>: filters by the server's owner ID, this uses the
	 * <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>version</u>: filters by the server's game version, this uses the
	 * <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>protocol</u>: filters by the server's game protocol version, this uses
	 * the <see cref="ServerListFilterType.STRICT"/> filtering type on the server
	 * side<br/>
	 * - <u>phoenixProtocol</u>: filters by the server's phoenix protocol version,
	 * this uses the <see cref="ServerListFilterType.STRICT"/> filtering type on the
	 * server side<br/>
	 * - <u>address</u>: searches the server address list for the IP address
	 * specified in the <paramref name="filterString"/><br/>
	 * - <u>players</u>: processed using a more complex parser, the
	 * <paramref name="filterString"/> is used to verify the player count (see below
	 * for details)<br/>
	 * <br/>
	 * <br/>
	 * Player count filtering:<br/>
	 * Phoenix allows for filtering servers by player count, this uses special
	 * operators specified in the <paramref name="filterString"/><br/>
	 * Note that between the operator and value, <u>there must not be a space</u>
	 * else it is not parsed. Example: `<u>&lt;=30</u>` (below or equal 30
	 * players)<br/>
	 * <br/>
	 * Here follows the list of operators and their behaviour:<br/>
	 * - <u>notfull</u> (exact filter string): checks if the server is not full<br/>
	 * - <u>&lt;<b>value</b></u>: checks if the player count is below
	 * <b>value</b><br/>
	 * - <u>&lt;=<b>value</b></u>: checks if the player count is below or equal to
	 * <b>value</b><br/>
	 * - <u>&gt;<b>value</b></u>: checks if the player count is greater than
	 * <b>value</b><br/>
	 * - <u>&gt;=<b>value</b></u>: checks if the player count is greater or equal to
	 * <b>value</b><br/>
	 * - <u><b>value</b></u>: checksc if the player count is equal to <b>value</b>
	 * </summary>
	 * 
	 * @param key          Filter key
	 * @param filterString Filter string
	 */
	public ServerListFilter(String key, String filterString) {
		this.filterType = ServerListFilterType.DEFAULT;
		this.key = key;
		this.value = filterString;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public ServerListFilterType getType() {
		return filterType;
	}

}
