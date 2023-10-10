package org.asf.edge.gameplayapi.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.xmls.achievements.AchievementRewardData;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardDefData;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardDefList;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.state.ItemStateData;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.services.quests.impl.QuestManagerImpl;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementRewardList;
import org.asf.edge.gameplayapi.xmls.achievements.StableQuestData;
import org.asf.edge.gameplayapi.xmls.achievements.StableQuestDragonAchievementBlock;
import org.asf.edge.gameplayapi.xmls.achievements.StableQuestRewardBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AchievementRewardDataConverter {

	public static void main(String[] args) throws JsonSyntaxException, IOException {
		// Expected arguments:
		// "<path to output xml>" <source paths...>

		// Prepare
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL, true);

		// Prepare output
		File outputFile = new File(args[0]);
		if (outputFile.getParentFile() != null)
			outputFile.getParentFile().mkdirs();
		AchievementRewardDefList rewardDataList = new AchievementRewardDefList();
		LinkedHashMap<Integer, AchievementRewardDefData> rewards = new LinkedHashMap<Integer, AchievementRewardDefData>();

		// Load existing if present
		if (outputFile.exists()) {
			rewardDataList = mapper.readValue(Files.readString(outputFile.toPath()), AchievementRewardDefList.class);
			if (rewardDataList.rewards != null) {
				for (AchievementRewardDefData def : rewardDataList.rewards) {
					rewards.put(def.achievementID, def);
				}
			}
		}

		// Load all data
		for (int i = 1; i < args.length; i++) {
			runForFiles(file -> {
				// Check file
				if (file.getName().equalsIgnoreCase("TimedMissionDO.xml")) {
					// Data from stable quests
					// Load stable quest data
					System.out.println("Importing: " + file);
					try {
						StableQuestData stData = mapper.readValue(Files.readString(file.toPath()),
								StableQuestData.class);
						if (stData.missions != null) {
							for (StableQuestData.MissionBlock data : stData.missions) {
								// Import win rewards

								// Load or create def
								AchievementRewardDefData def = rewards.get(data.winAchievementID);
								if (def == null)
									def = new AchievementRewardDefData();

								// Populate with existing data
								def.achievementID = data.winAchievementID;
								LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock> rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
								if (def.rewards != null) {
									for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
										rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
									}
								}

								// Import
								for (StableQuestRewardBlock reward : data.winRewards) {
									// Create or load entry
									AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
											.get(reward.rewardID + "-" + reward.pointTypeID);
									if (entry == null) {
										// Create
										entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
										entry.rewardID = reward.rewardID;
										entry.pointTypeID = reward.pointTypeID;
										entry.itemID = reward.itemID;
										rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
									}

									// Compute minimal and maximum values
									if (reward.amount < entry.minAmount || entry.minAmount == 0)
										entry.minAmount = reward.amount;
									if (reward.amount > entry.maxAmount)
										entry.maxAmount = reward.amount;
								}

								// Add rewards
								def.rewards = rewardEntries.values()
										.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

								// Save
								rewards.put(data.winAchievementID, def);

								// Generate victory rewards for non-default achievements
								for (StableQuestDragonAchievementBlock ach : data.winAchievements) {
									// Load or create def
									def = rewards.get(ach.achievementID);
									if (def == null)
										def = new AchievementRewardDefData();

									// Populate with existing data
									def.achievementID = ach.achievementID;
									rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
									if (def.rewards != null) {
										for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
											rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
										}
									}

									// Copy default achievements
									for (StableQuestRewardBlock reward : data.winRewards) {
										// Create or load entry
										AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
												.get(reward.rewardID + "-" + reward.pointTypeID);
										if (entry == null) {
											// Create
											entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
											entry.rewardID = reward.rewardID;
											entry.pointTypeID = reward.pointTypeID;
											entry.itemID = reward.itemID;
											rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
										}

										// Compute minimal and maximum values
										int am = reward.amount;
										if (reward.pointTypeID == 8)
											am = am / ach.dragonCount;
										if (am < entry.minAmount || entry.minAmount == 0)
											entry.minAmount = am;
										if (am > entry.maxAmount)
											entry.maxAmount = am;
									}

									// Add rewards
									def.rewards = rewardEntries.values()
											.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

									// Save
									rewards.put(ach.achievementID, def);
								}

								// Import lose rewards

								// Load or create def
								def = rewards.get(data.loseAchievementID);
								if (def == null)
									def = new AchievementRewardDefData();

								// Populate with existing data
								def.achievementID = data.loseAchievementID;
								rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
								if (def.rewards != null) {
									for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
										rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
									}
								}

								// Import
								for (StableQuestRewardBlock reward : data.loseRewards) {
									// Create or load entry
									AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
											.get(reward.rewardID + "-" + reward.pointTypeID);
									if (entry == null) {
										// Create
										entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
										entry.rewardID = reward.rewardID;
										entry.pointTypeID = reward.pointTypeID;
										entry.itemID = reward.itemID;
										rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
									}

									// Compute minimal and maximum values
									if (reward.amount < entry.minAmount || entry.minAmount == 0)
										entry.minAmount = reward.amount;
									if (reward.amount > entry.maxAmount)
										entry.maxAmount = reward.amount;
								}

								// Add rewards
								def.rewards = rewardEntries.values()
										.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

								// Save
								rewards.put(data.loseAchievementID, def);

								// Generate defeat rewards for non-default achievements
								for (StableQuestDragonAchievementBlock ach : data.loseAchievements) {
									// Load or create def
									def = rewards.get(ach.achievementID);
									if (def == null)
										def = new AchievementRewardDefData();

									// Populate with existing data
									def.achievementID = ach.achievementID;
									rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
									if (def.rewards != null) {
										for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
											rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
										}
									}

									// Copy default achievements
									for (StableQuestRewardBlock reward : data.loseRewards) {
										// Create or load entry
										AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
												.get(reward.rewardID + "-" + reward.pointTypeID);
										if (entry == null) {
											// Create
											entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
											entry.rewardID = reward.rewardID;
											entry.pointTypeID = reward.pointTypeID;
											entry.itemID = reward.itemID;
											rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
										}

										// Compute minimal and maximum values
										int am = reward.amount;
										if (reward.pointTypeID == 8)
											am = am / ach.dragonCount;
										if (am < entry.minAmount || entry.minAmount == 0)
											entry.minAmount = am;
										if (am > entry.maxAmount)
											entry.maxAmount = am;
									}

									// Add rewards
									def.rewards = rewardEntries.values()
											.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

									// Save
									rewards.put(ach.achievementID, def);
								}
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else if (file.getName().endsWith(".log")) {
					// Sniffs
					// Import data from sniff files
					System.out.println("Importing: " + file);

					// Read data into memory
					FileInputStream sourceFile = null;
					try {
						sourceFile = new FileInputStream(file);

						// Read file
						String buffer = "";
						while (true) {
							int b = sourceFile.read();
							if (b == -1)
								break;

							// Convert to byte
							byte bb = (byte) b;

							// Verify character
							char ch = (char) bb;
							if (ch == '\r')
								continue;
							else if (ch == '\n')
								continue;
							else if (ch == '{') {
								// Check character
								buffer = Character.toString(ch);

								// Read until end of block
								while (true) {
									byte[] bufferD = new byte[20480];
									int read = sourceFile.read(bufferD);
									if (read <= 0)
										break;

									// Go over buffer data
									String bData = new String(bufferD, "UTF-8");
									if (bData.contains("\n")) {
										buffer += bData.substring(0, bData.indexOf("\n"));
										sourceFile.getChannel().position(
												sourceFile.getChannel().position() - (read - bData.indexOf("\n")));
										break;
									} else
										buffer += bData;
								}

								// Done, verify JSON
								JsonObject obj = JsonParser.parseString(buffer).getAsJsonObject();

								// Check fields
								if ((!obj.has("time") || !obj.has("request") || !obj.has("response")
										|| !obj.get("request").getAsJsonObject().has("url")
										|| !obj.get("request").getAsJsonObject().has("method")
										|| !obj.get("request").getAsJsonObject().has("headers")
										|| !obj.get("request").getAsJsonObject().has("hasBody")
										|| !obj.get("response").getAsJsonObject().has("status")
										|| !obj.get("response").getAsJsonObject().has("statusMessage")
										|| !obj.get("response").getAsJsonObject().has("headers")
										|| !obj.get("response").getAsJsonObject().has("responseBody"))
										&& (!obj.has("type") || obj.get("type").getAsString().equals("http")))
									throw new IOException("Invalid file");
								if (obj.has("type") && !obj.get("type").getAsString().equals("http"))
									continue;

								// Verify request
								String url = obj.get("request").getAsJsonObject().get("url").getAsString();
								URL u = new URL(url);
								if (u.getPath()
										.equalsIgnoreCase("/AchievementWebService.asmx/SetUserAchievementAndGetReward")
										|| u.getPath().equalsIgnoreCase(
												"/AchievementWebService.asmx/SetAchievementAndGetReward")
										|| u.getPath().equalsIgnoreCase(
												"/AchievementWebService.asmx/SetAchievementByEntityIDs")) {

									// Decode request
									byte[] dec = Base64.getDecoder().decode(
											obj.get("request").getAsJsonObject().get("requestBody").getAsString());

									// Check encoding
									JsonObject headers = obj.get("request").getAsJsonObject().get("headers")
											.getAsJsonObject();
									if (headers.has("Content-Encoding")
											&& headers.get("Content-Encoding").getAsString().equalsIgnoreCase("gzip")) {
										GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(dec));
										dec = strm.readAllBytes();
										strm.close();
									}

									// Parse response
									String reqBody = new String(dec, "UTF-8");
									Map<String, String> payload = parseForm(reqBody);

									// Load achievement ID
									int achievementID = Integer.parseInt(payload.get("achievementID"));

									// Decode response
									dec = Base64.getDecoder().decode(
											obj.get("response").getAsJsonObject().get("responseBody").getAsString());

									// Check encoding
									headers = obj.get("response").getAsJsonObject().get("headers").getAsJsonObject();
									if (headers.has("Content-Encoding")
											&& headers.get("Content-Encoding").getAsString().equalsIgnoreCase("gzip")) {
										GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(dec));
										dec = strm.readAllBytes();
										strm.close();
									}

									// Parse response
									String resBody = new String(dec, "UTF-8");
									AchievementRewardList rewardData = mapper.readValue(resBody,
											AchievementRewardList.class);
									if (rewardData != null) {
										// Load or create def
										AchievementRewardDefData def = rewards.get(achievementID);
										if (def == null)
											def = new AchievementRewardDefData();

										// Populate with existing data
										def.achievementID = achievementID;
										LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock> rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
										if (def.rewards != null) {
											for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
												rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
											}
										}

										// Import rewards
										if (rewardData.rewards != null) {
											for (AchievementRewardData reward : rewardData.rewards) {
												// Create or load entry
												AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
														.get(reward.rewardID + "-" + reward.pointTypeID);
												if (entry == null) {
													// Create
													entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
													entry.rewardID = reward.rewardID;
													entry.pointTypeID = reward.pointTypeID;
													entry.itemID = reward.itemID;
													entry.allowMultiple = reward.allowMultiple;
													rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID,
															entry);
												}

												// Apply
												if (!entry.allowMultiple)
													entry.allowMultiple = reward.allowMultiple;

												// Compute minimal and maximum values
												int am = reward.amount;
												if (reward.pointTypeID == 1 || reward.pointTypeID == 8
														|| reward.pointTypeID == 12)
													am = am / 2;
												if (am < entry.minAmount || entry.minAmount == 0)
													entry.minAmount = am;
												if (reward.amount > entry.maxAmount)
													entry.maxAmount = am;
											}
										}

										// Add rewards
										def.rewards = rewardEntries.values().toArray(
												t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

										// Save
										rewards.put(achievementID, def);
									}
								}
//								else if (u.getPath()
//										.equalsIgnoreCase("/V2/AchievementWebService.asmx/SetUserAchievementTask")) {
//									// Decode request
//									byte[] dec = Base64.getDecoder().decode(
//											obj.get("request").getAsJsonObject().get("requestBody").getAsString());
//
//									// Check encoding
//									JsonObject headers = obj.get("request").getAsJsonObject().get("headers")
//											.getAsJsonObject();
//									if (headers.has("Content-Encoding")
//											&& headers.get("Content-Encoding").getAsString().equalsIgnoreCase("gzip")) {
//										GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(dec));
//										dec = strm.readAllBytes();
//										strm.close();
//									}
////
////									// Parse response
////									String reqBody = new String(dec, "UTF-8");
////									Map<String, String> payload = parseForm(reqBody);
////
////									// Compute key
////									String apiKey = payload.get("apiKey");
////									String secret = "56BB211B-CF06-48E1-9C1D-E40B5173D759";
////									if (EdgeWebService.API_SECRET_MAP.containsKey(apiKey.toUpperCase()))
////										secret = EdgeWebService.API_SECRET_MAP.get(apiKey.toUpperCase());
////									byte[] key;
////									try {
////										MessageDigest digest = MessageDigest.getInstance("MD5");
////										key = digest.digest(secret.getBytes("UTF-16LE"));
////									} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
////										throw new RuntimeException(e);
////									}
////
////									// Decrypt request
////									byte[] decrypt = TripleDesUtil.decrypt(
////											Base64.getDecoder().decode(payload.get("achievementTaskSetRequest")), key);
////									String reqPayloadBody = new String(decrypt, "UTF-16LE");
////									AchievementTaskSetRequestList req = mapper.readValue(reqPayloadBody,
////											AchievementTaskSetRequestList.class);
////
//									// Decode response
//									dec = Base64.getDecoder().decode(
//											obj.get("response").getAsJsonObject().get("responseBody").getAsString());
//
//									// Check encoding
//									headers = obj.get("response").getAsJsonObject().get("headers").getAsJsonObject();
//									if (headers.has("Content-Encoding")
//											&& headers.get("Content-Encoding").getAsString().equalsIgnoreCase("gzip")) {
//										GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(dec));
//										dec = strm.readAllBytes();
//										strm.close();
//									}
//
//									// Parse response
//									String resBody = new String(dec, "UTF-8");
//									AchievementTaskSetResponseList resp = mapper.readValue(resBody,
//											AchievementTaskSetResponseList.class);
//
//									// Go through requests
//									if (resp != null) {
//										for (AchievementTaskSetResponseData data : resp.responses) {
//											// Go over rewards
//											if (data.rewards != null) {
//												// Load achievement ID
//												int achievementID = data.achievementInfoID.value;
//
//												// Load or create def
//												AchievementRewardDefData def = rewards.get(achievementID);
//												if (def == null)
//													def = new AchievementRewardDefData();
//
//												// Populate with existing data
//												def.achievementID = achievementID;
//												LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock> rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
//												if (def.rewards != null) {
//													for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
//														rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID,
//																reward);
//													}
//												}
//
//												// Import rewards
//												for (AchievementRewardData reward : data.rewards) {
//													// Create or load entry
//													AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
//															.get(reward.rewardID + "-" + reward.pointTypeID);
//													if (entry == null) {
//														// Create
//														entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
//														entry.rewardID = reward.rewardID;
//														entry.pointTypeID = reward.pointTypeID;
//														entry.itemID = reward.itemID;
//														entry.allowMultiple = reward.allowMultiple;
//														rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID,
//																entry);
//													}
//
//													// Apply
//													if (!entry.allowMultiple)
//														entry.allowMultiple = reward.allowMultiple;
//
//													// Compute minimal and maximum values
//													int am = reward.amount;
//													if (reward.pointTypeID == 1 || reward.pointTypeID == 8
//															|| reward.pointTypeID == 12)
//														am = am / 2;
//													if (am < entry.minAmount || entry.minAmount == 0)
//														entry.minAmount = am;
//													if (reward.amount > entry.maxAmount)
//														entry.maxAmount = am;
//												}
//
//												// Add rewards
//												def.rewards = rewardEntries.values().toArray(
//														t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);
//
//												// Save
//												rewards.put(achievementID, def);
//											}
//										}
//									}
//								}
							} else
								throw new IOException("Invalid file");
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					try {
						sourceFile.close();
					} catch (IOException e) {
					}
				}
			}, new File(args[i]));
		}

		// Go over item achievements
		System.out.println("Finding item states...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);
		for (ItemInfo itm : ItemManager.getInstance().getAllItemDefinitions()) {
			ItemDefData iDef = itm.getRawObject();
			if (iDef.states != null) {
				for (ItemStateData state : iDef.states) {
					if (state.rewards != null && state.rewards.length != 0) {
						System.out.println("Importing rewards of: " + iDef.id + " state " + state.stateID);
						int achievementID = state.achievementID;

						// Load or create def
						AchievementRewardDefData def = rewards.get(achievementID);
						if (def == null)
							def = new AchievementRewardDefData();

						// Populate with existing data
						def.achievementID = achievementID;
						LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock> rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
						if (def.rewards != null) {
							for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
								rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
							}
						}

						// Import rewards
						for (AchievementRewardData reward : state.rewards) {
							// Create or load entry
							AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
									.get(reward.rewardID + "-" + reward.pointTypeID);
							if (entry == null) {
								// Create
								entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
								entry.rewardID = reward.rewardID;
								entry.pointTypeID = reward.pointTypeID;
								entry.itemID = reward.itemID;
								entry.allowMultiple = true;
								rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
							}

							// Apply
							entry.allowMultiple = true;

							// Compute minimal and maximum values
							if (reward.amount < entry.minAmount || entry.minAmount == 0)
								entry.minAmount = reward.amount;
							if (reward.amount > entry.maxAmount)
								entry.maxAmount = reward.amount;
						}

						// Add rewards
						def.rewards = rewardEntries.values()
								.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

						// Save
						rewards.put(achievementID, def);
					}
				}
			}
		}

		// Go over quest achievements
		System.out.println("Finding quest reward data...");
		ServiceManager.registerServiceImplementation(QuestManager.class, new QuestManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(QuestManager.class);
		for (MissionData qDef : QuestManager.getInstance().getAllQuestDefs()) {
			if ((qDef.acceptanceRewards != null && qDef.acceptanceRewards.length != 0)
					|| (qDef.rewards != null && qDef.rewards.length != 0)) {
				System.out.println("Importing rewards of: " + qDef.id);

				// Go through acceptance rewards
				if (qDef.acceptanceRewards != null) {
					int achievementID = qDef.acceptanceAchievementID;

					// Load or create def
					AchievementRewardDefData def = rewards.get(achievementID);
					if (def == null)
						def = new AchievementRewardDefData();

					// Populate with existing data
					def.achievementID = achievementID;
					LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock> rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
					if (def.rewards != null) {
						for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
							rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
						}
					}

					// Import rewards
					for (AchievementRewardData reward : qDef.acceptanceRewards) {
						// Create or load entry
						AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
								.get(reward.rewardID + "-" + reward.pointTypeID);
						if (entry == null) {
							// Create
							entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
							entry.rewardID = reward.rewardID;
							entry.pointTypeID = reward.pointTypeID;
							entry.itemID = reward.itemID;
							entry.allowMultiple = reward.allowMultiple;
							rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
						}

						// Apply
						if (!entry.allowMultiple)
							entry.allowMultiple = reward.allowMultiple;

						// Compute minimal and maximum values
						entry.minAmount = reward.minAmount;
						entry.maxAmount = reward.maxAmount;
					}

					// Add rewards
					def.rewards = rewardEntries.values()
							.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

					// Save
					rewards.put(achievementID, def);
				}

				// Go through rewards
				if (qDef.rewards != null) {
					int achievementID = qDef.achievementID;

					// Load or create def
					AchievementRewardDefData def = rewards.get(achievementID);
					if (def == null)
						def = new AchievementRewardDefData();

					// Populate with existing data
					def.achievementID = achievementID;
					LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock> rewardEntries = new LinkedHashMap<String, AchievementRewardDefData.AchievementRewardEntryBlock>();
					if (def.rewards != null) {
						for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
							rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, reward);
						}
					}

					// Import rewards
					for (AchievementRewardData reward : qDef.rewards) {
						// Create or load entry
						AchievementRewardDefData.AchievementRewardEntryBlock entry = rewardEntries
								.get(reward.rewardID + "-" + reward.pointTypeID);
						if (entry == null) {
							// Create
							entry = new AchievementRewardDefData.AchievementRewardEntryBlock();
							entry.rewardID = reward.rewardID;
							entry.pointTypeID = reward.pointTypeID;
							entry.itemID = reward.itemID;
							entry.allowMultiple = reward.allowMultiple;
							rewardEntries.put(reward.rewardID + "-" + reward.pointTypeID, entry);
						}

						// Compute minimal and maximum values
						entry.minAmount = reward.minAmount;
						entry.maxAmount = reward.maxAmount;
					}

					// Add rewards
					def.rewards = rewardEntries.values()
							.toArray(t -> new AchievementRewardDefData.AchievementRewardEntryBlock[t]);

					// Save
					rewards.put(achievementID, def);
				}
			}
		}

		// Save
		System.out.println("Imported " + rewards.size() + " rewards");
		rewardDataList.rewards = rewards.values().toArray(t -> new AchievementRewardDefData[t]);
		Files.writeString(outputFile.toPath(),
				mapper.writer().withDefaultPrettyPrinter().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
						.withRootName("RewardManifest").writeValueAsString(rewardDataList));
	}

	private static Map<String, String> parseForm(String payload) {
		HashMap<String, String> frm = new HashMap<String, String>();
		String key = "";
		String value = "";
		boolean isKey = true;
		for (int i = 0; i < payload.length(); i++) {
			char ch = payload.charAt(i);
			if (ch == '&') {
				if (isKey && !key.isEmpty()) {
					frm.put(key, "");
					key = "";
				} else if (!isKey && !key.isEmpty()) {
					try {
						frm.put(key, URLDecoder.decode(value, "UTF-8"));
					} catch (Exception e) {
						frm.put(key, value);
					}
					isKey = true;
					key = "";
					value = "";
				}
			} else if (ch == '=') {
				isKey = !isKey;
			} else {
				if (isKey) {
					key += ch;
				} else {
					value += ch;
				}
			}
		}
		if (!key.isEmpty() || !value.isEmpty()) {
			try {
				frm.put(key, URLDecoder.decode(value, "UTF-8"));
			} catch (Exception e) {
				frm.put(key, value);
			}
		}
		return frm;
	}

	private static void runForFiles(Consumer<File> callback, File source) {
		if (!source.exists())
			return;
		if (source.isFile()) {
			callback.accept(source);
			return;
		}
		for (File f : source.listFiles(t -> t.isDirectory()))
			runForFiles(callback, f);
		for (File f : source.listFiles(t -> !t.isDirectory()))
			runForFiles(callback, f);
	}

}
