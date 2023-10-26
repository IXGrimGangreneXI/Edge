package org.asf.edge.modules.gridapi.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonKvDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.identities.PropertyInfo;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class IdentityUtils {

	public static final Random rnd = new Random();

	/**
	 * Checks if a identity exists
	 * 
	 * @param identity Identity UUID
	 * @return True if the identity exists, false otherwise
	 */
	public static boolean identityExists(String identity) {
		// Verify ID
		try {
			UUID.fromString(identity);
		} catch (Exception e) {
			return false;
		}

		// Check
		try {
			if (CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES").entryExists("id-" + identity))
				return true;
			if (AccountManager.getInstance().accountExists(identity))
				return true;
			return AccountManager.getInstance().getSaveByID(identity) != null;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Deletes a identity
	 * 
	 * @param identity Identity UUID
	 * @return True if the identity was deleted, false otherwise
	 */
	public static boolean deleteIdentity(String identity) {
		// Verify ID
		try {
			UUID.fromString(identity);
		} catch (Exception e) {
			return false;
		}

		// Delete
		try {
			CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES");
			if (cont.entryExists("id-" + identity)) {
				cont.deleteEntry("id-" + identity);
				return true;
			}
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Updates a identity definition
	 * 
	 * @param identity   Identity UUID
	 * @param properties Identity properties
	 * @return True if successful, false otherwise
	 */
	public static boolean updateIdentity(String identity, Map<String, String> properties) {
		// Load old identity
		IdentityDef id = getIdentity(identity);
		if (id == null)
			return false;

		// Verify properties
		for (String key : properties.keySet()) {
			// Check validity
			if (!key.equals("displayName") && !key.equals("hostBanned")
					&& (!id.properties.containsKey(key) || id.properties.get(key).isReadonly))
				return false;
		}
		id.lastUpdateTime = System.currentTimeMillis();

		// Apply changes to properties
		for (String key : properties.keySet()) {
			if (!id.properties.containsKey(key))
				id.properties.put(key, new PropertyInfo());
			id.properties.get(key).value = properties.get(key);
		}

		// Save
		JsonObject obj = new JsonObject();
		long llu = obj.get("lastUpdate").getAsLong();
		obj.addProperty("lastUpdate", id.lastUpdateTime);
		long lsfn = obj.get("sfn").getAsLong();
		int lsfr = obj.get("sfr").getAsInt();
		obj.addProperty("sfn", id.significantFieldNumber);
		obj.addProperty("sfr", id.significantFieldRandom);
		JsonObject props = new JsonObject();
		id.properties.forEach((k, v) -> {
			JsonObject i = new JsonObject();
			i.addProperty("readOnly", v.isReadonly);
			i.addProperty("value", v.value);
			props.add(k, i);
		});
		obj.add("properties", props);
		try {
			if (AccountManager.getInstance().accountExists(identity)) {
				AccountObject acc = AccountManager.getInstance().getAccount(identity);
				AccountKvDataContainer cont = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
				if (properties.containsKey("displayName"))
					if (!acc.updateUsername(properties.get("displayName")))
						return false;
				cont.setEntry("phoenixidentity", obj);
				if (id.properties.containsKey("hostBanned"))
					cont.setEntry("hostBanned",
							new JsonPrimitive(id.properties.get("hostBanned").value.equalsIgnoreCase("true")));
				if (llu != id.lastUpdateTime)
					cont.setEntry("last_update", new JsonPrimitive(id.lastUpdateTime));
				if (lsfn != id.significantFieldNumber)
					cont.setEntry("significantFieldNumber", new JsonPrimitive(id.significantFieldNumber));
				if (lsfr != id.significantFieldRandom)
					cont.setEntry("significantFieldRandom", new JsonPrimitive(id.significantFieldRandom));
			} else {
				AccountSaveContainer sv = AccountManager.getInstance().getSaveByID(identity);
				if (sv != null) {
					if (properties.containsKey("displayName"))
						if (!sv.updateUsername(properties.get("displayName")))
							return false;
					AccountKvDataContainer cont = sv.getSaveData();
					cont.setEntry("phoenixidentity", obj);
					if (llu != id.lastUpdateTime)
						cont.setEntry("last_update", new JsonPrimitive(id.lastUpdateTime));
					if (lsfn != id.significantFieldNumber)
						cont.setEntry("significantFieldNumber", new JsonPrimitive(id.significantFieldNumber));
					if (lsfr != id.significantFieldRandom)
						cont.setEntry("significantFieldRandom", new JsonPrimitive(id.significantFieldRandom));
				} else {
					CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES");
					cont.setEntry("id-" + identity, obj);
				}
			}
		} catch (IOException e) {
		}

		return true;
	}

	/**
	 * Updates a identity definition
	 * 
	 * @param id Identity object
	 */
	public static void updateIdentity(IdentityDef id) {
		// Save
		JsonObject obj = new JsonObject();
		obj.addProperty("lastUpdate", id.lastUpdateTime);
		obj.addProperty("sfn", id.significantFieldNumber);
		obj.addProperty("sfr", id.significantFieldRandom);
		JsonObject props = new JsonObject();
		id.properties.forEach((k, v) -> {
			JsonObject i = new JsonObject();
			i.addProperty("readOnly", v.isReadonly);
			i.addProperty("value", v.value);
			props.add(k, i);
		});
		obj.add("properties", props);
		try {
			if (AccountManager.getInstance().accountExists(id.identity)) {
				AccountObject acc = AccountManager.getInstance().getAccount(id.identity);
				AccountKvDataContainer cont = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
				if (id.properties.containsKey("displayName"))
					acc.updateUsername(id.properties.get("displayName").value);
				cont.setEntry("phoenixidentity", obj);
				if (id.properties.containsKey("hostBanned"))
					cont.setEntry("hostBanned",
							new JsonPrimitive(id.properties.get("hostBanned").value.equalsIgnoreCase("true")));
				cont.setEntry("last_update", new JsonPrimitive(id.lastUpdateTime));
				cont.setEntry("significantFieldNumber", new JsonPrimitive(id.significantFieldNumber));
				cont.setEntry("significantFieldRandom", new JsonPrimitive(id.significantFieldRandom));
			} else {
				AccountSaveContainer sv = AccountManager.getInstance().getSaveByID(id.identity);
				if (sv != null) {
					if (id.properties.containsKey("displayName"))
						sv.updateUsername(id.properties.get("displayName").value);
					AccountKvDataContainer cont = sv.getSaveData();
					cont.setEntry("phoenixidentity", obj);
					cont.setEntry("last_update", new JsonPrimitive(id.lastUpdateTime));
					cont.setEntry("significantFieldNumber", new JsonPrimitive(id.significantFieldNumber));
					cont.setEntry("significantFieldRandom", new JsonPrimitive(id.significantFieldRandom));
				} else {
					CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES");
					cont.setEntry("id-" + id.identity, obj);
				}
			}
		} catch (IOException e) {
		}
	}

	/**
	 * Creates a identity definition
	 * 
	 * @param properties Identity properties
	 * @return IdentityDef instance
	 */
	public static IdentityDef createIdentity(Map<String, PropertyInfo> properties) {
		// Create identity object
		IdentityDef identity = new IdentityDef();
		identity.significantFieldNumber = System.currentTimeMillis();
		identity.significantFieldRandom = rnd.nextInt();

		// Create identity ID
		identity.identity = UUID.randomUUID().toString();
		while (identityExists(identity.identity)) {
			identity.identity = UUID.randomUUID().toString();
		}

		// Save properties to object
		identity.properties.putAll(properties);
		identity.lastUpdateTime = System.currentTimeMillis();

		// Save
		CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES");
		JsonObject obj = new JsonObject();
		obj.addProperty("lastUpdate", identity.lastUpdateTime);
		obj.addProperty("sfn", identity.significantFieldNumber);
		obj.addProperty("sfr", identity.significantFieldRandom);
		JsonObject props = new JsonObject();
		properties.forEach((k, v) -> {
			JsonObject i = new JsonObject();
			i.addProperty("readOnly", v.isReadonly);
			i.addProperty("value", v.value);
			props.add(k, i);
		});
		obj.add("properties", props);
		try {
			cont.setEntry("id-" + identity.identity, obj);
		} catch (IOException e) {
		}

		// Return object
		return identity;
	}

	/**
	 * Retrieves a identity by ID
	 * 
	 * @param identity Identity UUID
	 * @return IdentityDef instance or null
	 */
	public static IdentityDef getIdentity(String identity) {
		// Verify ID
		try {
			UUID.fromString(identity);
		} catch (Exception e) {
			return null;
		}

		// Find file
		CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES");
		try {
			if (cont.entryExists("id-" + identity)) {
				// Read identity
				JsonObject obj = cont.getEntry("id-" + identity).getAsJsonObject();

				// Create object
				IdentityDef def = new IdentityDef();
				def.identity = identity;
				def.lastUpdateTime = obj.get("lastUpdate").getAsLong();
				def.significantFieldNumber = obj.get("sfn").getAsLong();
				def.significantFieldRandom = obj.get("sfr").getAsInt();

				// Load properties
				JsonObject props = obj.get("properties").getAsJsonObject();
				props.keySet().forEach(key -> {
					JsonObject prop = props.get(key).getAsJsonObject();
					PropertyInfo info = new PropertyInfo();
					info.isReadonly = prop.get("readOnly").getAsBoolean();
					info.value = prop.get("value").getAsString();
					def.properties.put(key, info);
				});

				// Return object
				return def;
			} else {
				// Find account
				if (AccountManager.getInstance().accountExists(identity)) {
					// Read identity
					JsonObject obj = null;
					AccountObject acc = AccountManager.getInstance().getAccount(identity);
					AccountKvDataContainer dC = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
					if (dC.entryExists("phoenixidentity"))
						obj = dC.getEntry("phoenixidentity").getAsJsonObject();
					else {
						// Create
						obj = new JsonObject();
						if (!dC.entryExists("significantFieldRandom"))
							dC.setEntry("significantFieldRandom", new JsonPrimitive(IdentityUtils.rnd.nextInt()));
						if (!dC.entryExists("significantFieldNumber"))
							dC.setEntry("significantFieldNumber", new JsonPrimitive(System.currentTimeMillis()));
						if (!dC.entryExists("last_update"))
							dC.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));
						obj.addProperty("lastUpdate", dC.getEntry("last_update").getAsLong());
						obj.addProperty("sfn", dC.getEntry("significantFieldNumber").getAsLong());
						obj.addProperty("sfr", dC.getEntry("significantFieldRandom").getAsInt());
						JsonObject props = new JsonObject();
						JsonObject i = new JsonObject();
						i.addProperty("readOnly", true);
						i.addProperty("value", acc.getAccountID());
						props.add("name", i);
						i = new JsonObject();
						i.addProperty("readOnly", false);
						i.addProperty("value", acc.getUsername());
						props.add("displayName", i);
						obj.add("properties", props);

						// Save
						dC.setEntry("phoenixidentity", obj);
					}

					// Create object
					IdentityDef def = new IdentityDef();
					def.identity = identity;
					def.lastUpdateTime = dC.getEntry("last_update").getAsLong();
					def.significantFieldNumber = dC.getEntry("significantFieldNumber").getAsLong();
					def.significantFieldRandom = dC.getEntry("significantFieldRandom").getAsInt();

					// Load properties
					JsonObject props = obj.get("properties").getAsJsonObject();
					props.keySet().forEach(key -> {
						JsonObject prop = props.get(key).getAsJsonObject();
						PropertyInfo info = new PropertyInfo();
						info.isReadonly = prop.get("readOnly").getAsBoolean();
						info.value = prop.get("value").getAsString();
						def.properties.put(key, info);
					});
					def.properties.get("displayName").value = acc.getUsername();

					// Return object
					return def;
				} else {
					// Read identity
					JsonObject obj = null;
					AccountSaveContainer sv = AccountManager.getInstance().getSaveByID(identity);
					if (sv != null) {
						AccountKvDataContainer dC = sv.getSaveData();
						if (dC.entryExists("phoenixidentity"))
							obj = dC.getEntry("phoenixidentity").getAsJsonObject();
						else {
							// Create
							obj = new JsonObject();
							if (!dC.entryExists("significantFieldRandom"))
								dC.setEntry("significantFieldRandom", new JsonPrimitive(IdentityUtils.rnd.nextInt()));
							if (!dC.entryExists("significantFieldNumber"))
								dC.setEntry("significantFieldNumber", new JsonPrimitive(System.currentTimeMillis()));
							if (!dC.entryExists("last_update"))
								dC.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));
							obj.addProperty("lastUpdate", dC.getEntry("last_update").getAsLong());
							obj.addProperty("sfn", dC.getEntry("significantFieldNumber").getAsLong());
							obj.addProperty("sfr", dC.getEntry("significantFieldRandom").getAsInt());
							JsonObject props = new JsonObject();
							JsonObject i = new JsonObject();
							i.addProperty("readOnly", true);
							i.addProperty("value", sv.getSaveID());
							props.add("name", i);
							i = new JsonObject();
							i.addProperty("readOnly", false);
							i.addProperty("value", sv.getUsername());
							props.add("displayName", i);
							obj.add("properties", props);

							// Save
							dC.setEntry("phoenixidentity", obj);
						}

						// Create object
						IdentityDef def = new IdentityDef();
						def.identity = identity;
						def.lastUpdateTime = dC.getEntry("last_update").getAsLong();
						def.significantFieldNumber = dC.getEntry("significantFieldNumber").getAsLong();
						def.significantFieldRandom = dC.getEntry("significantFieldRandom").getAsInt();

						// Load properties
						JsonObject props = obj.get("properties").getAsJsonObject();
						props.keySet().forEach(key -> {
							JsonObject prop = props.get(key).getAsJsonObject();
							PropertyInfo info = new PropertyInfo();
							info.isReadonly = prop.get("readOnly").getAsBoolean();
							info.value = prop.get("value").getAsString();
							def.properties.put(key, info);
						});
						def.properties.get("displayName").value = sv.getUsername();

						// Return object
						return def;
					}
				}
			}
		} catch (JsonSyntaxException | IOException e) {
			return null;
		}

		// Not found
		return null;
	}

	/**
	 * Retrieves all identity definitions for a game (THIS PROCESS IS INTENSIVE)
	 * 
	 * @param game Game defnition
	 * @return Array of IdentityDef instances
	 */
	public static IdentityDef[] getAll() {
		ArrayList<IdentityDef> defs = new ArrayList<IdentityDef>();
		CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("PHOENIXIDENTITIES");
		try {
			cont.runForEntries((key, value) -> {
				if (key.startsWith("id-")) {
					// Read identity
					JsonObject obj = value.getAsJsonObject();

					// Create object
					IdentityDef def = new IdentityDef();
					def.identity = key.substring(3);
					def.lastUpdateTime = obj.get("lastUpdate").getAsLong();
					def.significantFieldNumber = obj.get("sfn").getAsLong();
					def.significantFieldRandom = obj.get("sfr").getAsInt();

					// Load properties
					JsonObject props = obj.get("properties").getAsJsonObject();
					props.keySet().forEach(key2 -> {
						JsonObject prop = props.get(key2).getAsJsonObject();
						PropertyInfo info = new PropertyInfo();
						info.isReadonly = prop.get("readOnly").getAsBoolean();
						info.value = prop.get("value").getAsString();
						def.properties.put(key2, info);
					});
					defs.add(def);
				}
				return true;
			});
		} catch (IOException e) {
		}
		return defs.toArray(t -> new IdentityDef[t]);
	}

}
