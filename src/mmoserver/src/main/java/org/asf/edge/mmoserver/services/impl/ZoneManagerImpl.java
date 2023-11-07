package org.asf.edge.mmoserver.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;
import org.asf.edge.mmoserver.entities.smartfox.RoomVariable;
import org.asf.edge.mmoserver.entities.smartfox.VariableType;
import org.asf.edge.mmoserver.events.zones.ZoneCreatedEvent;
import org.asf.edge.mmoserver.events.zones.ZoneDeletedEvent;
import org.asf.edge.mmoserver.events.zones.ZoneManagerLoadEvent;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventBus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ZoneManagerImpl extends ZoneManager {

	private Logger logger;
	private HashMap<String, GameZone> zones = new HashMap<String, GameZone>();

	@Override
	protected void managerLoaded() {
		// Log
		logger.info("Zone manager loaded successfully!");

		// Dispatch event
		logger.info("Dispatching load event...");
		EventBus.getInstance().dispatchEvent(new ZoneManagerLoadEvent(this));
	}

	@Override
	protected void prepareManager() {
		// Init logger
		logger = LogManager.getLogger("ZoneManager");
		logger.info("Preparing zone manager...");
	}

	@Override
	protected void loadZonesFromDisk() {
		// Load zones from disk
		logger.info("Loading zone configuration...");

		// Generate defaults
		JsonObject confDefault = new JsonObject();
		JsonObject zonesBlockDefault = new JsonObject();
		JsonObject groupsBlockDefault = new JsonObject();
		confDefault.add("zones", zonesBlockDefault);
		JsonObject jsZone = new JsonObject();
		jsZone.addProperty("active", true);
		jsZone.add("roomGroups", groupsBlockDefault);
		zonesBlockDefault.add("JumpStart", jsZone);
		zonesBlockDefault.add("ProjectEdge", jsZone);

		// Populate defaults
		JsonObject defaultGroupBlockDefault = new JsonObject();
		groupsBlockDefault.add("default", defaultGroupBlockDefault);
		JsonObject roomsDefaultBlock = new JsonObject();
		defaultGroupBlockDefault.add("rooms", roomsDefaultBlock);

		// MP_SYS
		JsonObject mpSysRoom = new JsonObject();
		mpSysRoom.addProperty("isGame", true);
		mpSysRoom.addProperty("isHidden", false);
		mpSysRoom.addProperty("isPasswordProtected", false);
		mpSysRoom.addProperty("maxUsers", 10);
		JsonArray varsMpSys = new JsonArray();
		mpSysRoom.add("variables", varsMpSys);
		mpSysRoom.addProperty("maxSpectators", 0);
		roomsDefaultBlock.add("MP_SYS", mpSysRoom);

		// ADMIN
		JsonObject adminRoom = new JsonObject();
		adminRoom.addProperty("isGame", false);
		adminRoom.addProperty("isHidden", false);
		adminRoom.addProperty("isPasswordProtected", true);
		adminRoom.addProperty("maxUsers", 1);
		JsonArray varsAdmin = new JsonArray();

		JsonObject varWEScoutAttack = new JsonObject();
		varWEScoutAttack.addProperty("name", "WE_ScoutAttack");
		varWEScoutAttack.addProperty("dynamic", true);
		varWEScoutAttack.addProperty("dynamicAssignmentKey", "sod.rooms.admin.vars.we_scoutattack");
		varWEScoutAttack.addProperty("private", false);
		varsAdmin.add(varWEScoutAttack);

		JsonObject varWENScoutAttack = new JsonObject();
		varWENScoutAttack.addProperty("name", "WEN_ScoutAttack");
		varWENScoutAttack.addProperty("dynamic", true);
		varWENScoutAttack.addProperty("dynamicAssignmentKey", "sod.rooms.admin.vars.wen_scoutattack");
		varWENScoutAttack.addProperty("private", false);
		varsAdmin.add(varWENScoutAttack);

		adminRoom.add("variables", varsAdmin);
		roomsDefaultBlock.add("ADMIN", adminRoom);

		// LIMBO
		JsonObject limboRoom = new JsonObject();
		limboRoom.addProperty("isGame", false);
		limboRoom.addProperty("isHidden", false);
		limboRoom.addProperty("isPasswordProtected", false);
		limboRoom.addProperty("maxUsers", 10000);
		JsonArray varsLimbo = new JsonArray();
		limboRoom.add("variables", varsLimbo);
		roomsDefaultBlock.add("LIMBO", limboRoom);

		try {
			// Load config
			JsonObject config = ConfigProviderService.getInstance().loadConfig("server", "mmorooms", confDefault);

			// Load zones
			JsonObject zones = config.get("zones").getAsJsonObject();
			for (String zoneName : zones.keySet()) {
				// Load
				logger.info("Loading saved zone: " + zoneName + "...");
				JsonObject zoneInfo = zones.get(zoneName).getAsJsonObject();

				// Create
				GameZone zone = createZone(zoneName, zoneInfo.get("active").getAsBoolean());

				// Create room groups
				if (zoneInfo.has("roomGroups")) {
					logger.info("Loading saved zone room groups of zone " + zoneName + "...");
					JsonObject groups = zoneInfo.get("roomGroups").getAsJsonObject();

					// Go through groups
					for (String groupName : groups.keySet()) {
						// Load group
						JsonObject groupData = groups.get(groupName).getAsJsonObject();
						logger.info("Loading saved room group " + groupName + " of zone " + zoneName + "...");
						RoomGroup group = zone.addRoomGroup(groupName);

						// Load rooms
						if (groupData.has("rooms")) {
							// Load room data
							JsonObject roomList = groupData.get("rooms").getAsJsonObject();
							logger.info("Loading saved rooms of group " + groupName + " in zone " + zoneName + "...");

							// Go through rooms
							for (String roomName : roomList.keySet()) {
								JsonObject roomDetails = roomList.get(roomName).getAsJsonObject();

								// Load details
								boolean isGame = roomDetails.get("isGame").getAsBoolean();
								boolean isHidden = roomDetails.get("isHidden").getAsBoolean();
								boolean isPasswordProtected = roomDetails.get("isPasswordProtected").getAsBoolean();
								short maxUsers = roomDetails.get("maxUsers").getAsShort();
								short maxSpectators = 0;
								if (isGame)
									maxSpectators = roomDetails.get("maxSpectators").getAsShort();

								// Load variables
								ArrayList<RoomVariable> variables = new ArrayList<RoomVariable>();
								JsonArray vars = roomDetails.get("variables").getAsJsonArray();
								for (JsonElement ele : vars) {
									JsonObject varInfo = ele.getAsJsonObject();

									// Create variable
									RoomVariable var;
									if (!varInfo.has("dynamic") || !varInfo.get("dynamic").getAsBoolean()) {
										// Load value
										VariableType type;
										Object value = null;
										switch (varInfo.get("type").getAsString()) {

										case "null": {
											type = VariableType.NULL;
											break;
										}

										case "boolean": {
											type = VariableType.BOOLEAN;
											value = varInfo.get("value").getAsBoolean();
											break;
										}

										case "integer": {
											type = VariableType.INTEGER;
											value = varInfo.get("value").getAsInt();
											break;
										}

										case "double": {
											type = VariableType.DOUBLE;
											value = varInfo.get("value").getAsDouble();
											break;
										}

										case "string": {
											type = VariableType.STRING;
											value = varInfo.get("value").getAsString();
											break;
										}

										case "object": {
											type = VariableType.OBJECT;
											SmartfoxPayload pl = new SmartfoxPayload();
											value = pl;

											// Load object
											encodeObject(pl, varInfo.get("value").getAsJsonObject());
											break;
										}

										case "array": {
											type = VariableType.ARRAY;
											value = encodeArr(varInfo.get("value").getAsJsonArray());
											break;
										}

										default: {
											throw new IOException("Unrecognized value type: "
													+ varInfo.get("type").getAsString() + " for variable "
													+ varInfo.get("name").getAsString() + " of room " + roomName
													+ " (group: " + groupName + ", zone: " + zoneName + ")");
										}

										}

										// Create
										var = new RoomVariable(null, varInfo.get("name").getAsString(), type, value,
												varInfo.get("private").getAsBoolean(), true);
									} else {
										// Create
										var = new RoomVariable(null, varInfo.get("name").getAsString(),
												varInfo.get("dynamicAssignmentKey").getAsString(),
												varInfo.get("private").getAsBoolean(), true);
									}

									// Add
									variables.add(var);
								}

								// Create room
								group.addGameRoom(roomName, isHidden, isPasswordProtected, maxUsers,
										variables.toArray(t -> new RoomVariable[t]), maxSpectators);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failed to load MMO room configuration!", e);
		}
	}

	private void encodeObject(SmartfoxPayload pl, JsonObject obj) {
		for (String key : obj.keySet()) {
			JsonElement val = obj.get(key);
			encodeVal(key, val, pl);
		}
	}

	private void encodeVal(String key, JsonElement val, SmartfoxPayload pl) {
		// Check type
		if (val.isJsonObject()) {
			// Object
			SmartfoxPayload ch = new SmartfoxPayload();
			encodeObject(ch, val.getAsJsonObject());
			pl.setObject(key, ch);
		} else if (val.isJsonArray()) {
			// Array
			Object[] arr = encodeArr(val.getAsJsonArray());
			pl.setObjectArray(key, arr);
		} else if (val.isJsonPrimitive()) {
			// Primitive
			JsonPrimitive prim = (JsonPrimitive) val;
			if (prim.isBoolean()) {
				// Boolean
				pl.setBoolean(key, prim.getAsBoolean());
			} else if (prim.isNumber()) {
				// Number
				Number num = prim.getAsNumber();
				if (num.doubleValue() != num.longValue()) {
					// Double
					pl.setDouble(key, num.doubleValue());
				} else {
					// Other
					if (num.longValue() > Integer.MAX_VALUE || num.longValue() < Integer.MIN_VALUE) {
						pl.setLong(key, num.longValue());
					} else if (num.intValue() > Short.MAX_VALUE || num.intValue() < Short.MIN_VALUE) {
						pl.setInt(key, num.intValue());
					} else if (num.shortValue() > Byte.MAX_VALUE || num.shortValue() < Byte.MIN_VALUE) {
						pl.setShort(key, num.shortValue());
					} else {
						pl.setByte(key, num.byteValue());
					}
				}
			} else {
				// String
				pl.setString(key, prim.getAsString());
			}
		}
	}

	private Object[] encodeArr(JsonArray arrJ) {
		int i = 0;
		Object[] arr = new Object[arrJ.size()];
		for (JsonElement ele : arrJ) {
			// Check type
			if (ele.isJsonObject()) {
				// Object
				SmartfoxPayload ch = new SmartfoxPayload();
				encodeObject(ch, ele.getAsJsonObject());
				arr[i++] = ch;
			} else if (ele.isJsonArray()) {
				// Array
				arr[i++] = encodeArr(ele.getAsJsonArray());
			} else if (ele.isJsonPrimitive()) {
				// Primitive
				JsonPrimitive prim = (JsonPrimitive) ele;
				if (prim.isBoolean()) {
					// Boolean
					arr[i++] = prim.getAsBoolean();
				} else if (prim.isNumber()) {
					// Number
					Number num = prim.getAsNumber();
					if (num.doubleValue() != num.longValue()) {
						// Double
						arr[i++] = num.doubleValue();
					} else {
						// Other
						if (num.longValue() > Integer.MAX_VALUE || num.longValue() < Integer.MIN_VALUE) {
							arr[i++] = num.longValue();
						} else if (num.intValue() > Short.MAX_VALUE || num.intValue() < Short.MIN_VALUE) {
							arr[i++] = num.intValue();
						} else if (num.shortValue() > Byte.MAX_VALUE || num.shortValue() < Byte.MIN_VALUE) {
							arr[i++] = num.shortValue();
						} else {
							arr[i++] = num.byteValue();
						}
					}
				} else {
					// String
					arr[i++] = prim.getAsString();
				}
			}
		}
		return arr;
	}

	@Override
	public GameZone[] getZones() {
		synchronized (zones) {
			return zones.values().toArray(t -> new GameZone[t]);
		}
	}

	@Override
	public String[] getZoneNames() {
		synchronized (zones) {
			return zones.values().stream().map(t -> t.getName()).toArray(t -> new String[t]);
		}
	}

	@Override
	public GameZone getZone(String name) {
		synchronized (zones) {
			return zones.get(name);
		}
	}

	@Override
	public void removeZone(GameZone zone) {
		synchronized (zones) {
			if (zones.containsKey(zone.getName())) {
				// Log
				logger.info("Removing zone: " + zone.getName());

				// Remove all groups in zone
				for (RoomGroup gr : zone.getRoomGroups())
					zone.removeRoomGroup(gr);

				// Remove
				zones.remove(zone.getName());

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new ZoneDeletedEvent(this, zone));
			}
		}
	}

	@Override
	public GameZone createZone(String name, boolean isActive, RoomGroup[] groups) throws IllegalArgumentException {
		synchronized (zones) {
			if (zones.containsKey(name))
				throw new IllegalArgumentException("Zone already exists: " + name);

			// Log
			logger.info("Creating zone: " + name);

			// Create zone
			GameZone zone = new GameZone(name, isActive, groups);

			// Add
			zones.put(name, zone);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new ZoneCreatedEvent(this, zone));

			// Return
			return zone;
		}
	}

}
