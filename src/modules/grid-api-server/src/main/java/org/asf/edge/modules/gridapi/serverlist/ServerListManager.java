package org.asf.edge.modules.gridapi.serverlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;

public class ServerListManager {

	private static ArrayList<ServerListEntry> entries = new ArrayList<ServerListEntry>();

	public static void onPostServerUpdate(String identity, Map<String, String> entries) {
		ServerListEntry entry = getServer(identity);
		if (entry != null) {
			entry.entries = entries;
			entry.lastEntryUpdate = System.currentTimeMillis();
		}
	}

	public static void onPostServer(IdentityDef identity, RemoteClient socket, int port, String[] addresses,
			String version, int protocolVersion, int phoenixProtocolVersion) {
		ServerPostHandler handler = new ServerPostHandler(identity, socket);
		handler.start(() -> {
			if (getServer(identity.identity) == null) {
				ServerListEntry entry = new ServerListEntry();
				entry.serverId = identity.identity;
				entry.listConnection = handler;
				entry.port = port;
				entry.addresses = addresses;
				entry.ownerId = identity.properties.get("owner").value;
				entry.version = version;
				entry.protocolVersion = protocolVersion;
				entry.phoenixProtocolVersion = phoenixProtocolVersion;
				if (getServer(identity.identity) == null)
					entries.add(entry);
				else
					handler.state = true;
				try {
					entry.requestUpdate();
				} catch (IOException e) {
				}
			} else
				handler.state = true;
		});
		ServerListEntry entry = getServer(identity.identity);
		if (entry != null && !handler.state) {
			entries.remove(entry);
		}
	}

	public static ServerListEntry getServer(String id) {
		for (ServerListEntry entry : getServers()) {
			if (entry.serverId.equals(id))
				return entry;
		}
		return null;
	}

	public static ServerListEntry[] getServers() {
		ServerListEntry[] servers;
		while (true) {
			try {
				servers = ServerListManager.entries.toArray(t -> new ServerListEntry[t]);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Filter banned
		ArrayList<ServerListEntry> srvLst = new ArrayList<ServerListEntry>();
		for (ServerListEntry ent : servers) {
			// Check if the owner is not host-banned
			if (!ent.ownerId.equals(new UUID(0, 0).toString())) {
				// Attempt to find account
				AccountObject acc = AccountManager.getInstance().getAccount(ent.ownerId);
				if (acc != null) {
					try {
						AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
						if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
							// Banned from hosting
							continue;
						}
					} catch (IOException e) {
						continue;
					}
				} else {
					// Attempt to find identity
					IdentityDef ownerDef = IdentityUtils.getIdentity(ent.ownerId);
					if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
							&& ownerDef.properties.get("hostBanned").value.equals("true")) {
						// Banned from hosting
						continue;
					} else if (ownerDef == null) {
						// Owner was deleted
						IdentityUtils.deleteIdentity(ent.serverId);
						continue;
					}
				}
			}

			// Add
			srvLst.add(ent);
		}

		// Return
		return srvLst.toArray(t -> new ServerListEntry[t]);
	}

	public static ServerListEntry[] getServers(Map<String, String> filter) {
		if (filter.size() == 0)
			return getServers();
		while (true) {
			try {
				ServerListEntry[] entries = getServers();
				entries = Stream.of(entries).filter(t -> {
					// Check filter
					for (String key : filter.keySet()) {
						if (key.equals("id")) {
							if (!t.serverId.equals(filter.get(key)))
								return false;
						} else if (key.equals("version")) {
							if (!t.version.equalsIgnoreCase(filter.get(key)))
								return false;
						} else if (key.equals("protocol")) {
							if (!Integer.toString(t.protocolVersion).equalsIgnoreCase(filter.get(key)))
								return false;
						} else if (key.equals("phoenixProtocol")) {
							if (!Integer.toString(t.phoenixProtocolVersion).equalsIgnoreCase(filter.get(key)))
								return false;
						} else if (key.equals("ownerId")) {
							if (!t.ownerId.equals(filter.get(key)))
								return false;
						} else if (key.equals("port")) {
							if (!Integer.toString(t.port).equals(filter.get(key)))
								return false;
						} else if (key.equals("address")) {
							if (!Stream.of(t.addresses).anyMatch(t2 -> t2.equals(filter.get(key))))
								return false;
						} else if (key.equals("players")) {
							// Check count
							try {
								if (!t.entries.containsKey("players.current"))
									return false;
								int max = -1;
								int current = Integer.parseInt(t.entries.get("players.current"));
								String countRequest = filter.get(key);
								if (t.entries.containsKey("players.max")) {
									max = Integer.parseInt(t.entries.get("players.max"));
								} else {
									if (countRequest.equalsIgnoreCase("notfull"))
										return true;
								}
								if (countRequest.equalsIgnoreCase("notfull")) {
									// Check player count
									if (current >= max)
										return false;
								} else if (countRequest.startsWith("<=")) {
									max = Integer.parseInt(countRequest.substring(2));
									if (current > max)
										return false;
								} else if (countRequest.startsWith("<")) {
									max = Integer.parseInt(countRequest.substring(1));
									if (current >= max)
										return false;
								} else if (countRequest.startsWith(">=")) {
									max = Integer.parseInt(countRequest.substring(2));
									if (current < max)
										return false;
								} else if (countRequest.startsWith(">")) {
									max = Integer.parseInt(countRequest.substring(1));
									if (current <= max)
										return false;
								} else {
									if (current != Integer.parseInt(countRequest))
										return false;
								}
							} catch (NumberFormatException e) {
								return false;
							}
						} else if (key.startsWith("==")) {
							return t.entries.containsKey(key.substring(2)) && t.entries.get(key.substring(2))
									.toLowerCase().equals(filter.get(key).toLowerCase());
						} else if (key.startsWith("=~")) {
							return t.entries.containsKey(key.substring(2)) && t.entries.get(key.substring(2))
									.toLowerCase().contains(filter.get(key).toLowerCase());
						} else if (key.startsWith("!=")) {
							return !t.entries.containsKey(key.substring(2)) && t.entries.get(key.substring(2))
									.toLowerCase().equals(filter.get(key).toLowerCase());
						} else if (key.startsWith("!~")) {
							return !t.entries.containsKey(key.substring(2)) && t.entries.get(key.substring(2))
									.toLowerCase().contains(filter.get(key).toLowerCase());
						} else if (!t.entries.containsKey(key)
								|| !t.entries.get(key).toLowerCase().contains(filter.get(key).toLowerCase()))
							return false;
					}
					return true;
				}).toArray(t -> new ServerListEntry[t]);
				return entries;
			} catch (ConcurrentModificationException e) {
			}
		}
	}

}
