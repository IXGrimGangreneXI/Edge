package org.asf.edge.common.services.achievements.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.entities.achivements.RankMultiplierInfo;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.commondata.CommonKvDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.xmls.achievements.AchievementRewardData;
import org.asf.edge.common.xmls.achievements.UserRankData;
import org.asf.edge.common.xmls.achievements.UserRankList;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardDefData;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardDefData.AchievementRewardEntryBlock;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardDefList;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardTransformerData;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardTransformerData.TransformerRewardEntryBlock;
import org.asf.edge.common.xmls.items.inventory.InventoryItemEntryData;
import org.asf.edge.common.xmls.achievements.edgespecific.AchievementRewardTransformerList;
import org.asf.edge.common.xmls.dragons.DragonData;
import org.asf.edge.common.events.achievements.AchievementManagerLoadEvent;
import org.asf.edge.common.experiments.EdgeDefaultExperiments;
import org.asf.edge.common.experiments.ExperimentManager;
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class AchievementManagerImpl extends AchievementManager {

	private Logger logger;
	private HashMap<Integer, RankInfo> ranks = new HashMap<Integer, RankInfo>();
	private HashMap<Integer, AchievementRewardEntryBlock[]> rewards = new HashMap<Integer, AchievementRewardEntryBlock[]>();

	private long lastReloadTime;

	private static Random rnd = new Random();

	@Override
	public void initService() {
		logger = LogManager.getLogger("AchievementManager");

		// Start reload watchdog
		CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("ACHIEVEMENTMANAGER");
		try {
			if (!cont.entryExists("lastreload")) {
				lastReloadTime = System.currentTimeMillis();
				cont.setEntry("lastreload", new JsonPrimitive(lastReloadTime));
			} else
				lastReloadTime = cont.getEntry("lastreload").getAsLong();
		} catch (IOException e) {
		}
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Check reload
				try {
					long reload = cont.getEntry("lastreload").getAsLong();
					if (reload > lastReloadTime) {
						// Trigger reload
						lastReloadTime = reload;
						loadData();
					}
				} catch (IOException e) {
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});

		// Load data
		loadData();
	}

	private void loadData() {
		// Prepare
		HashMap<Integer, RankInfo> ranks = new HashMap<Integer, RankInfo>();
		HashMap<Integer, AchievementRewardEntryBlock[]> rewards = new HashMap<Integer, AchievementRewardEntryBlock[]>();

		// Load ranks
		logger.info("Loading rank definitions...");

		try {
			// Load XML
			InputStream strm = getClass().getClassLoader().getResourceAsStream("achievementdata/ranks.xml");
			String data = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Load into map
			XmlMapper mapper = new XmlMapper();
			UserRankList rs = mapper.reader().readValue(data, UserRankList.class);

			// Load ranks
			for (UserRankData.UserRankDataWrapper rank : rs.ranks) {
				ranks.put(rank.rankID.value, new RankInfo(rank.getUnwrapped()));
				logger.debug("Registered rank: " + rank.getUnwrapped().rankID + ": " + rank.getUnwrapped().rankName);

			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Load rank transformers
		logger.info("Loading rank transformers...");
		loadTransformers(getClass(), ranks);

		// Load module transformers
		for (IEdgeModule module : ModuleManager.getLoadedModules()) {
			loadTransformers(module.getClass(), ranks);
		}

		// Load all transformers from disk
		File transformersRanks = new File("ranktransformers");
		if (transformersRanks.exists()) {
			for (File transformer : transformersRanks.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
				loadRankTransformer(transformer, ranks);
			}
		}

		// Apply
		this.ranks = ranks;

		// Load achievement rewards if needed
		if (ExperimentManager.getInstance().isExperimentEnabled(EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT)) {
			// Load achievement data
			logger.info("Loading achievement system V1 reward data...");

			try {
				// Load XML
				InputStream strm = getClass().getClassLoader()
						.getResourceAsStream("achievementdata/achievementrewards.xml");
				String data = new String(strm.readAllBytes(), "UTF-8");
				strm.close();

				// Load into map
				XmlMapper mapper = new XmlMapper();
				AchievementRewardDefList rs = mapper.reader().readValue(data, AchievementRewardDefList.class);

				// Load ranks
				for (AchievementRewardDefData def : rs.rewards) {
					// Verify reward defs
					for (AchievementRewardDefData.AchievementRewardEntryBlock reward : def.rewards) {
						if (Stream.of(def.rewards)
								.filter(t -> t.rewardID == reward.rewardID && t.pointTypeID == reward.pointTypeID)
								.count() != 1) {
							// Error
							throw new IOException("Reward def " + def.achievementID
									+ " has multiple rewards with the same reward ID with the same point type ID");
						}
					}

					// Add
					rewards.put(def.achievementID, def.rewards);
					logger.debug("Registered achievement: " + def.achievementID + ": " + def.rewards.length
							+ " rewards registered");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Load rank transformers
			logger.info("Loading achievement transformers...");
			loadTransformersAchievements(getClass(), rewards);

			// Load module transformers
			for (IEdgeModule module : ModuleManager.getLoadedModules()) {
				loadTransformersAchievements(module.getClass(), rewards);
			}

			// Load all transformers from disk
			File transformersAchievements = new File("rewardtransformers");
			if (transformersAchievements.exists()) {
				for (File transformer : transformersAchievements
						.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
					loadAchievementTransformer(transformer, rewards);
				}
			}

			// Apply
			this.rewards = rewards;
		}

		// Fire event
		logger.info("Dispatching load event...");
		EventBus.getInstance().dispatchEvent(new AchievementManagerLoadEvent(this));
	}

	private void loadTransformers(Class<?> cls, HashMap<Integer, RankInfo> ranks) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			logger.debug("Loading transformers from " + fileName + "...");
			InputStream strm = new URL(baseURL + "ranktransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				logger.debug("Loading transformer: 'ranktransformers/" + ele.getAsString() + ".xml'...");
				try {
					// Find the transformer
					strm = new URL(baseURL + "ranktransformers/" + ele.getAsString() + ".xml").openStream();

					// Load transformer
					XmlMapper mapper = new XmlMapper();
					UserRankData def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"),
							UserRankData.class);
					strm.close();

					// Define if needed
					if (!ranks.containsKey(def.rankID)) {
						// Register
						ranks.put(def.rankID, new RankInfo(def));
						logger.debug("Registered rank: " + def.rankID + ": " + def.rankName);
					} else {
						// Update
						RankInfo r = ranks.get(def.rankID);
						if (r == null)
							throw new IllegalArgumentException("Rank definition not found: " + def.rankID);

						// Load data
						UserRankData raw = r.getRawObject();
						raw.audio = def.audio;
						raw.globalRankID = def.globalRankID;
						raw.image = def.image;
						raw.pointTypeID = def.pointTypeID;
						raw.rankDescription = def.rankDescription;
						raw.rankID = def.rankID;
						raw.rankName = def.rankName;
						raw.value = def.value;

						// Reload
						r.reload();
						logger.debug("Updated rank: " + r.getID() + ": " + r.getName());
					}
				} catch (Exception e) {
					logger.error("Transformer failed to load: " + ele.getAsString() + " (" + fileName + ")", e);
				}
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException)
				return;
			throw new RuntimeException(e);
		}

	}

	private void loadRankTransformer(File transformer, HashMap<Integer, RankInfo> ranks) {
		if (transformer.isFile()) {
			logger.debug("Loading transformer: '" + transformer.getPath() + "'...");
			try {
				// Find the transformer
				InputStream strm = new FileInputStream(transformer);

				// Load transformer
				XmlMapper mapper = new XmlMapper();
				UserRankData def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"),
						UserRankData.class);
				strm.close();

				// Define if needed
				if (!ranks.containsKey(def.rankID)) {
					// Register
					ranks.put(def.rankID, new RankInfo(def));
					logger.debug("Registered rank: " + def.rankID + ": " + def.rankName);
				} else {
					// Update
					RankInfo r = ranks.get(def.rankID);
					if (r == null)
						throw new IllegalArgumentException("Rank definition not found: " + def.rankID);

					// Load data
					UserRankData raw = r.getRawObject();
					raw.audio = def.audio;
					raw.globalRankID = def.globalRankID;
					raw.image = def.image;
					raw.pointTypeID = def.pointTypeID;
					raw.rankDescription = def.rankDescription;
					raw.rankID = def.rankID;
					raw.rankName = def.rankName;
					raw.value = def.value;

					// Reload
					r.reload();
					logger.debug("Updated rank: " + r.getID() + ": " + r.getName());
				}
			} catch (Exception e) {
				logger.error("Transformer failed to load: " + transformer.getPath(), e);
			}
		} else {
			logger.debug("Loading transformers from " + transformer.getPath() + "...");
			for (File tr : transformer.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
				loadRankTransformer(tr, ranks);
			}
		}
	}

	private void loadTransformersAchievements(Class<?> cls, HashMap<Integer, AchievementRewardEntryBlock[]> rewards) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			logger.debug("Loading transformers from " + fileName + "...");
			InputStream strm = new URL(baseURL + "rewardtransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				logger.debug("Loading transformer: 'rewardtransformers/" + ele.getAsString() + ".xml'...");
				try {
					// Find the transformer
					strm = new URL(baseURL + "rewardtransformers/" + ele.getAsString() + ".xml").openStream();

					// Load transformer
					XmlMapper mapper = new XmlMapper();
					AchievementRewardTransformerList transformers = mapper.reader().readValue(
							new String(strm.readAllBytes(), "UTF-8"), AchievementRewardTransformerList.class);
					strm.close();

					// Apply
					applyTransformers(transformers, rewards);
				} catch (Exception e) {
					logger.error("Transformer failed to load: " + ele.getAsString() + " (" + fileName + ")", e);
				}
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException)
				return;
			throw new RuntimeException(e);
		}

	}

	private void applyTransformers(AchievementRewardTransformerList transformers,
			HashMap<Integer, AchievementRewardEntryBlock[]> rewards) throws IOException {
		// Go through transformers
		for (AchievementRewardTransformerData def : transformers.transformers) {
			// Check mode
			//
			// Modes:
			// - Define (creates new reward definitions)
			// - Merge (adds or replaces rewards in the list)
			// - Replace (replaces the reward list)
			// - Remove (removes rewards)
			//
			if (!def.mode.equalsIgnoreCase("define") && !def.mode.equalsIgnoreCase("merge")
					&& !def.mode.equalsIgnoreCase("replace") && !def.mode.equalsIgnoreCase("merge"))
				throw new IOException(
						"Invalid transformer mode: " + def.mode + ", expected either Define, Merge, Replace or Remove");

			// Define if needed
			if (!rewards.containsKey(def.achievementID) && def.mode.equalsIgnoreCase("define")) {
				// Register
				logger.debug("Applying Define-mode transformer to " + def.achievementID);
				rewards.put(def.achievementID, def.rewards);
				logger.debug("Registered achievement: " + def.achievementID + ": " + def.rewards.length
						+ " rewards registered");
			} else {
				// Check mode
				if (def.mode.equalsIgnoreCase("define"))
					throw new IOException("Unable to define new reward: " + def.achievementID
							+ ": reward list already exists, please use a different transformer mode");

				// Check existence
				if (!rewards.containsKey(def.achievementID))
					throw new IOException("Unable to find reward: " + def.achievementID
							+ ": reward not found, please use a different transformer mode");

				// Check mode
				if (def.mode.equalsIgnoreCase("replace")) {
					// Replace def data
					logger.debug("Applying Replace-mode transformer to " + def.achievementID);
					rewards.put(def.achievementID, def.rewards);
					logger.debug("Replaced achievement rewards for " + def.achievementID + ": " + def.rewards.length
							+ " rewards registered");
				} else if (def.mode.equalsIgnoreCase("remove")) {
					// Remove rewards

					// Create list
					logger.debug("Applying Remove-mode transformer to " + def.achievementID);
					ArrayList<AchievementRewardEntryBlock> newRewards = new ArrayList<AchievementRewardEntryBlock>();
					newRewards.addAll(Arrays.asList(rewards.get(def.achievementID)));

					// Verify each reward
					int i = 1;
					for (AchievementRewardEntryBlock removeDef : def.rewards) {
						if (removeDef.rewardID == -1 || removeDef.pointTypeID == -1)
							throw new IOException("Reward transformer #" + i + " for achievement def "
									+ def.achievementID
									+ " is missing RewardID and PointTypeID, cannot remove reward defs without those fields");

						// Find and remove reward entry
						for (AchievementRewardEntryBlock reward : newRewards) {
							if (reward.rewardID == removeDef.rewardID && reward.pointTypeID == removeDef.pointTypeID) {
								// Found it
								newRewards.remove(reward);
								logger.debug("Removed reward: " + reward.rewardID + ":" + reward.pointTypeID);
								break;
							}
						}

						// Increase
						i++;
					}

					// Apply
					rewards.put(def.achievementID, newRewards.toArray(t -> new AchievementRewardEntryBlock[t]));
					logger.debug("Replaced achievement rewards for " + def.achievementID + ": " + def.rewards.length
							+ " rewards registered");
				} else {
					// Log
					logger.debug("Applying Merge-mode transformer to " + def.achievementID);
					ArrayList<AchievementRewardEntryBlock> newRewards = new ArrayList<AchievementRewardEntryBlock>();
					newRewards.addAll(Arrays.asList(rewards.get(def.achievementID)));

					// Verify each reward
					int i = 1;
					for (TransformerRewardEntryBlock mergeDef : def.rewards) {
						// Find reward transforming target
						AchievementRewardEntryBlock target = null;
						if (mergeDef.rewardID != -1 && mergeDef.pointTypeID != -1) {
							// Find reward entry
							for (AchievementRewardEntryBlock reward : newRewards) {
								if (reward.rewardID == mergeDef.rewardID
										&& reward.pointTypeID == mergeDef.pointTypeID) {
									// Found it
									target = reward;
									break;
								}
							}
						} else if (mergeDef.pointTypeID != -1
								&& ((mergeDef.minAmount != -1 && mergeDef.maxAmount != -1) || mergeDef.amount != -1)) {
							// Find reward entry
							for (AchievementRewardEntryBlock reward : newRewards) {
								if (reward.pointTypeID == mergeDef.pointTypeID) {
									// Check amount tags
									if (mergeDef.amount != -1) {
										if (mergeDef.amount < reward.minAmount || mergeDef.amount > reward.maxAmount)
											continue; // No match
									} else if (mergeDef.minAmount != -1 && mergeDef.maxAmount != -1) {
										if (mergeDef.minAmount < reward.minAmount
												|| mergeDef.maxAmount > reward.maxAmount)
											continue; // No match
									}

									// Found it
									target = reward;
									break;
								}
							}
						} else if (mergeDef.itemID != -1) {
							// Find reward entry
							for (AchievementRewardEntryBlock reward : newRewards) {
								if (reward.itemID == mergeDef.itemID) {
									// Check amount tags
									if (mergeDef.amount != -1) {
										if (mergeDef.amount < reward.minAmount || mergeDef.amount > reward.maxAmount)
											continue; // No match
									} else if (mergeDef.minAmount != -1 && mergeDef.maxAmount != -1) {
										if (mergeDef.minAmount < reward.minAmount
												|| mergeDef.maxAmount > reward.maxAmount)
											continue; // No match
									}

									// Found it
									target = reward;
									break;
								}
							}
						} else {
							throw new IOException("Reward transformer #" + i + " for achievement def "
									+ def.achievementID + " is missing RewardID, PointTypeID and ItemID,"
									+ " please add use one of the following structures for targeting:"
									+ " RewardID and PointTypeID, PointTypeID and Amount, ItemID and Amount"
									+ " or ItemID for transformer targeting");
						}

						// Update transformer def
						if (mergeDef.transformItemID != -1)
							mergeDef.itemID = mergeDef.transformItemID;
						if (mergeDef.transformPointTypeID != -1)
							mergeDef.pointTypeID = mergeDef.transformPointTypeID;
						if (mergeDef.transformMinAmount != -1)
							mergeDef.minAmount = mergeDef.transformMinAmount;
						if (mergeDef.transformMaxAmount != -1)
							mergeDef.maxAmount = mergeDef.transformMaxAmount;

						// Handle target
						if (target == null) {
							// Define reward
							if (mergeDef.pointTypeID == -1)
								throw new IOException("Reward transformer #" + i + " for achievement def "
										+ def.achievementID
										+ " is missing value for PointTypeID, unable to define it as a new reward!");
							if (mergeDef.pointTypeID == 6 && mergeDef.itemID == -1)
								throw new IOException(
										"Reward transformer #" + i + " for achievement def " + def.achievementID
												+ " is missing value for ItemID, unable to define it as a new reward!");
							if (mergeDef.maxAmount == -1 && mergeDef.minAmount == -1)
								throw new IOException("Reward transformer #" + i + " for achievement def "
										+ def.achievementID
										+ " is missing value for MinAmount and MaxAmount, unable to define it as a new reward!");

							// Check ID
							if (mergeDef.rewardID == -1) {
								// Generate ID that isnt in use
								int id = 100;
								while (true) {
									int idF = id;
									if (!Stream.of(def.rewards).anyMatch(t -> t.rewardID == idF))
										break;
									id++;
								}

								// Apply
								mergeDef.rewardID = id;
							}

							// Create
							newRewards.add(mergeDef);
							logger.debug("Created reward: " + mergeDef.rewardID + ":" + mergeDef.pointTypeID);
						} else {
							// Apply merge
							if (mergeDef.itemID != -1)
								target.itemID = mergeDef.itemID;
							if (mergeDef.maxAmount != -1)
								target.maxAmount = mergeDef.maxAmount;
							if (mergeDef.minAmount != -1)
								target.minAmount = mergeDef.minAmount;
							if (mergeDef.pointTypeID != -1)
								target.pointTypeID = mergeDef.pointTypeID;
							if (mergeDef.transformAllowMultiple != null)
								target.allowMultiple = mergeDef.transformAllowMultiple.equalsIgnoreCase("true");
							logger.debug("Updated reward: " + mergeDef.rewardID + ":" + mergeDef.pointTypeID);
						}

						// Increase
						i++;
					}

					// Apply
					rewards.put(def.achievementID, newRewards.toArray(t -> new AchievementRewardEntryBlock[t]));
					logger.debug("Replaced achievement rewards for " + def.achievementID + ": " + def.rewards.length
							+ " rewards registered");
				}
			}
		}
	}

	private void loadAchievementTransformer(File transformer, HashMap<Integer, AchievementRewardEntryBlock[]> rewards) {
		if (transformer.isFile()) {
			logger.debug("Loading transformer: '" + transformer.getPath() + "'...");
			try {
				// Find the transformer
				InputStream strm = new FileInputStream(transformer);

				// Load transformer
				XmlMapper mapper = new XmlMapper();
				AchievementRewardTransformerList transformers = mapper.reader()
						.readValue(new String(strm.readAllBytes(), "UTF-8"), AchievementRewardTransformerList.class);
				strm.close();

				// Apply
				applyTransformers(transformers, rewards);
			} catch (Exception e) {
				logger.error("Transformer failed to load: " + transformer.getPath(), e);
			}
		} else {
			logger.debug("Loading transformers from " + transformer.getPath() + "...");
			for (File tr : transformer.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
				loadRankTransformer(tr, ranks);
			}
		}
	}

	@Override
	public RankInfo[] getRankDefinitions() {
		return Stream.of(ranks.values().toArray(t -> new RankInfo[t]))
				.sorted((t1, t2) -> Integer.compare(t1.getValue(), t2.getValue())).toArray(t -> new RankInfo[t]);
	}

	@Override
	public RankInfo getRankDefinition(int id) {
		return ranks.get(id);
	}

	@Override
	public void registerRankDefinition(int id, UserRankData def) {
		// Check
		if (ranks.containsKey(id))
			throw new IllegalArgumentException("Rank definition already exists " + id);
		def.rankID = id;

		// Register
		ranks.put(id, new RankInfo(def));
		logger.debug("Registered rank: " + def.rankID + ": " + def.rankName);
	}

	@Override
	public void updateRankDefinition(int id, UserRankData def) {
		// Find def
		RankInfo r = getRankDefinition(id);
		if (r == null)
			throw new IllegalArgumentException("Rank definition not found: " + id);

		// Load data
		UserRankData raw = r.getRawObject();
		raw.audio = def.audio;
		raw.globalRankID = def.globalRankID;
		raw.image = def.image;
		raw.pointTypeID = def.pointTypeID;
		raw.rankDescription = def.rankDescription;
		raw.rankID = def.rankID;
		raw.rankName = def.rankName;
		raw.value = def.value;

		// Reload
		r.reload();
		logger.debug("Updated rank: " + r.getID() + ": " + r.getName());

	}

	@Override
	public void reload() {
		// Reload
		loadData();
	}

	@Override
	public RankInfo[] getRankDefinitionsByPointType(int pointTypeID) {
		return Stream.of(getRankDefinitions()).filter(t -> t.getPointTypeID() == pointTypeID)
				.toArray(t -> new RankInfo[t]);
	}

	@Override
	public EntityRankInfo getRankForUser(AccountSaveContainer save, RankTypeID type) {
		return new UserRankContainer(type, save);
	}

	@Override
	public EntityRankInfo getRankForDragon(AccountSaveContainer save, String dragonEntityID) {
		try {
			return new DragonRankContainer(save, dragonEntityID);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public EntityRankInfo getRankForClan(AccountSaveContainer save, String clanID) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public int applyModifiers(AccountSaveContainer save, int value, RankTypeID type) {
		// Find factor
		int factor = 1;
		boolean first = true;

		// Go through each multiplier
		for (RankMultiplierInfo m : getRankMultipliers(save)) {
			if (m.isValid() && m.getPointType().getPointTypeID() == type.getPointTypeID()) {
				if (first)
					factor = m.getMultiplicationFactor();
				else
					factor += m.getMultiplicationFactor();
				first = false;
			}
		}

		// Return
		return value * factor;
	}

	@Override
	public synchronized RankMultiplierInfo[] getServerwideRankMultipliers() {
		// Load multipliers from disk
		SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		if (!ConfigProviderService.getInstance().configExists("server", "multipliers")) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR, 12);
			cal.set(Calendar.MINUTE, 00);
			cal.add(Calendar.DAY_OF_MONTH, 10);
			Date t = cal.getTime();

			// Generate
			try {
				JsonObject conf = new JsonObject();
				conf.addProperty("__COMMENT__", "This file defines the active multipliers used in the game.");
				conf.addProperty("__COMMENT2__",
						"The example below is generated at first run and is configured to create 3 multipliers that are valid for 10 days.");
				JsonArray ml = new JsonArray();
				conf.add("multipliers", ml);
				JsonObject m1 = new JsonObject();
				m1.addProperty("type", 1);
				m1.addProperty("factor", 2);
				m1.addProperty("expiry", fmt.format(t));
				ml.add(m1);
				JsonObject m2 = new JsonObject();
				m2.addProperty("type", 12);
				m2.addProperty("factor", 2);
				m2.addProperty("expiry", fmt.format(t));
				ml.add(m2);
				JsonObject m3 = new JsonObject();
				m3.addProperty("type", 8);
				m3.addProperty("factor", 2);
				m3.addProperty("expiry", fmt.format(t));
				ml.add(m3);
				ConfigProviderService.getInstance().saveConfig("server", "multipliers", conf);
			} catch (IOException e) {
				logger.error("Failed to save rank multipliers", e);
			}
		}

		// Read config
		ArrayList<RankMultiplierInfo> multipliers = new ArrayList<RankMultiplierInfo>();
		try {
			JsonObject conf = ConfigProviderService.getInstance().loadConfig("server", "multipliers");
			for (JsonElement ele : conf.get("multipliers").getAsJsonArray()) {
				JsonObject m = ele.getAsJsonObject();

				// Parse time
				Date t = fmt.parse(m.get("expiry").getAsString());

				// Create object
				int typeID = m.get("type").getAsInt();
				int factor = m.get("factor").getAsInt();
				multipliers.add(new RankMultiplierInfo(RankTypeID.getByTypeID(typeID), factor, t.getTime()));
			}
		} catch (IOException | ParseException e) {
			logger.error("Failed to load rank multipliers", e);
		}

		// Return
		return multipliers.toArray(t -> new RankMultiplierInfo[t]);
	}

	@Override
	public RankMultiplierInfo[] getUserRankMultipliers(AccountSaveContainer save) {
		// Read save data
		ArrayList<RankMultiplierInfo> multipliers = new ArrayList<RankMultiplierInfo>();
		try {
			AccountKvDataContainer rewardMultipliers = save.getSaveData().getChildContainer("reward_multipliers");
			if (!rewardMultipliers.entryExists("active"))
				rewardMultipliers.setEntry("active", new JsonArray());

			// Load multipliers
			boolean edited = false;
			JsonArray lst = rewardMultipliers.getEntry("active").getAsJsonArray();
			ArrayList<JsonObject> multipliersToRemove = new ArrayList<JsonObject>();
			for (JsonElement ele : lst) {
				JsonObject multiplierInfo = ele.getAsJsonObject();

				// Check expiry
				if (multiplierInfo.get("expiry").getAsLong() < System.currentTimeMillis()) {
					edited = true;
					multipliersToRemove.add(multiplierInfo);
				}

				// Add
				multipliers.add(new RankMultiplierInfo(RankTypeID.getByTypeID(multiplierInfo.get("typeID").getAsInt()),
						multiplierInfo.get("factor").getAsInt(), multiplierInfo.get("expiry").getAsLong()));
			}

			// Save
			if (edited) {
				for (JsonObject m : multipliersToRemove)
					lst.remove(m);
				rewardMultipliers.setEntry("active", lst);
			}
		} catch (IOException e) {
			logger.error("Failed to load user rank multipliers", e);
		}

		// Return
		return multipliers.toArray(t -> new RankMultiplierInfo[t]);
	}

	@Override
	public void addUserRankMultiplier(AccountSaveContainer save, RankMultiplierInfo multiplier) {
		try {
			AccountKvDataContainer rewardMultipliers = save.getSaveData().getChildContainer("reward_multipliers");
			if (!rewardMultipliers.entryExists("active"))
				rewardMultipliers.setEntry("active", new JsonArray());

			// Find multiplier
			JsonObject multiplierJ = null;
			JsonArray lst = rewardMultipliers.getEntry("active").getAsJsonArray();
			for (JsonElement ele : lst) {
				JsonObject multiplierInfo = ele.getAsJsonObject();

				// Check
				if (multiplierInfo.get("typeID").getAsInt() == multiplier.getPointType().getPointTypeID())
					multiplierJ = multiplierInfo;
			}
			if (multiplierJ == null) {
				multiplierJ = new JsonObject();
				lst.add(multiplierJ);
			}

			// Add fields
			multiplierJ.addProperty("typeID", multiplier.getPointType().getPointTypeID());
			multiplierJ.addProperty("factor", multiplier.getMultiplicationFactor());
			multiplierJ.addProperty("expiry", multiplier.getExpiryTime());

			// Save
			rewardMultipliers.setEntry("active", lst);
		} catch (IOException e) {
			logger.error("Failed to add user rank multiplier", e);
		}
	}

	@Override
	public boolean hasUnlockedAchievement(AccountSaveContainer save, int achievementID) {
		try {
			AccountKvDataContainer data = save.getSaveData().getChildContainer("achievements-v1");
			return data.entryExists("unlocked-" + achievementID)
					&& data.getEntry("unlocked-" + achievementID).getAsBoolean();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public AchievementRewardData[] giveAchievementRewards(AccountSaveContainer save, int achievementID,
			boolean wasGivenBefore, String... entityIDs) {
		try {
			// Find defs
			if (!rewards.containsKey(achievementID)) {
				// Error

				// Log the warning
				logger.warn("No achievement rewards available for " + achievementID + ", unable to give any rewards!");

				// Send a player message
				WsGenericMessage msg = new WsGenericMessage();
				msg.rawObject.typeID = 3;
				msg.rawObject.messageContentMembers = "A problem occurred with Edge gameplay server logic, there were no achievement rewards available for achievement ID "
						+ achievementID + " and as a result no rewards could be given.\n\n"
						+ "Please report this as a bug, include what you were doing when you got this message and what was missing rewards."
						+ " Note this message has likely been delayed due to how the client works, please include details surrounding the time when you saw this message.";
				msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
				WsMessageService.getInstance().getMessengerFor(save.getAccount(), save).sendSessionMessage(msg);

				// Return
				return new AchievementRewardData[0];
			}

			// Create list
			ArrayList<AchievementRewardData> rewards = new ArrayList<AchievementRewardData>();
			AchievementRewardEntryBlock[] rewardData = this.rewards.get(achievementID);
			String rewardStr = "";

			// Go through rewards
			for (AchievementRewardEntryBlock rewardDef : rewardData) {
				if (!rewardDef.allowMultiple && wasGivenBefore)
					continue;

				// Copy
				AchievementRewardData reward = new AchievementRewardData();
				reward.achievementID = achievementID;
				reward.pointTypeID = rewardDef.pointTypeID;
				reward.rewardID = rewardDef.rewardID;
				reward.amount = rewardDef.minAmount;
				reward.minAmount = rewardDef.minAmount;
				reward.maxAmount = rewardDef.maxAmount;
				reward.itemID = rewardDef.itemID;
				reward.allowMultiple = rewardDef.allowMultiple;

				// Set ID
				reward.entityID = UUID.fromString(save.getSaveID());

				// Set amount
				if (reward.minAmount != -1 && reward.maxAmount != -1) {
					reward.amount = rnd.nextInt(reward.minAmount, reward.maxAmount + 1);
				}

				// Give rewards
				switch (reward.pointTypeID) {

				// Rank points
				case 1:
				case 9:
				case 8:
				case 10:
				case 11:
				case 12: {
					// Check type ID
					if (reward.pointTypeID != 8 || entityIDs.length == 0) {
						// Use amount and type ID
						// Find type ID
						RankTypeID id = RankTypeID.getByTypeID(reward.pointTypeID);

						// Check
						String userID = rankUserID(save, id);
						reward.entityID = UUID.fromString(userID);
						reward.achievementID = achievementID;

						// Add XP
						EntityRankInfo r = AchievementManager.getInstance().getRank(save, userID, id);
						if (userID != null && r != null)
							reward.amount = r.addPoints(reward.amount);

						// Add to reward string
						if (!rewardStr.isEmpty())
							rewardStr += ", ";
						rewardStr += reward.amount + " " + id + " points";
					} else {
						int totalA = 0;

						// Use amount and type ID
						// Find type ID
						RankTypeID id = RankTypeID.getByTypeID(reward.pointTypeID);

						// Add for each dragon
						for (String dragonID : entityIDs) {
							// Add reward if needed
							if (reward.entityID != null)
								rewards.add(reward);

							// Copy reward
							reward = reward.copy();

							// Check
							reward.entityID = UUID.fromString(dragonID);
							reward.achievementID = achievementID;

							// Add XP
							EntityRankInfo r = AchievementManager.getInstance().getRank(save, dragonID, id);
							if (r != null)
								reward.amount = r.addPoints(reward.amount);
							totalA += reward.amount;
						}

						// Add to reward string
						if (!rewardStr.isEmpty())
							rewardStr += ", ";
						rewardStr += totalA + " " + id + " points";
					}

					break;
				}

				// Coins
				case 2: {
					// Update inventory
					AccountKvDataContainer currency = save.getSaveData().getChildContainer("currency");
					int currentC = 300;
					if (currency.entryExists("coins"))
						currentC = currency.getEntry("coins").getAsInt();
					currency.setEntry("coins", new JsonPrimitive(currentC + reward.amount));
					reward.achievementID = achievementID;

					// Add to reward string
					if (!rewardStr.isEmpty())
						rewardStr += ", ";
					rewardStr += reward.amount + " coins";
					break;
				}

				// Gems
				case 5: {
					// Update inventory
					AccountKvDataContainer currencyAccWide = save.getAccount().getAccountKeyValueContainer()
							.getChildContainer("currency");
					int currentG = 0;
					if (currencyAccWide.entryExists("gems"))
						currentG = currencyAccWide.getEntry("gems").getAsInt();
					currencyAccWide.setEntry("gems", new JsonPrimitive(currentG + reward.amount));
					reward.achievementID = achievementID;

					// Add to reward string
					if (!rewardStr.isEmpty())
						rewardStr += ", ";
					rewardStr += reward.amount + " gems";
					break;
				}

				// Item
				case 6: {
					// Add item
					PlayerInventory inv = save.getInventory();
					PlayerInventoryContainer cont = inv.getContainer(1);
					PlayerInventoryItem itm = cont.findFirst(reward.itemID);
					if (itm != null) {
						itm.add(reward.amount);
					} else {
						itm = cont.createItem(reward.itemID, reward.amount);
					}

					// Set data
					reward.achievementID = achievementID;
					reward.uniqueRewardItemID = itm.getUniqueID();

					// Add item
					InventoryItemEntryData block = new InventoryItemEntryData();
					block.itemID = itm.getItemDefID();
					block.quantity = itm.getQuantity();
					block.uses = itm.getUses();
					block.uniqueItemID = itm.getUniqueID();
					block.itemAttributes = itm.getAttributes().toAttributeData();
					// TODO: tier and attributes

					// Add data info from item manager
					ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
					if (def != null)
						block.data = def.getRawObject();

					// Set block
					reward.rewardItem = block;

					// Add to reward string
					if (!rewardStr.isEmpty())
						rewardStr += ", ";
					rewardStr += reward.amount + " "
							+ (reward.amount == 1 ? def.getRawObject().name : def.getRawObject().namePlural);
					break;
				}

				}

				// Add reward
				rewards.add(reward);
			}

			// Return
			logger.info("Player " + save.getUsername() + " (ID " + save.getSaveID() + ")"
					+ " received rewards of achievement " + achievementID + ", gave " + rewards.size() + " rewards. ("
					+ rewardStr + ")");
			return rewards.toArray(t -> new AchievementRewardData[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void unlockAchievement(AccountSaveContainer save, int achievementID) {
		try {
			AccountKvDataContainer data = save.getSaveData().getChildContainer("achievements-v1");
			data.setEntry("unlocked-" + achievementID, new JsonPrimitive(true));
			logger.info("Player " + save.getUsername() + " (ID " + save.getSaveID() + ")" + " unlocked achievement "
					+ achievementID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String rankUserID(AccountSaveContainer save, RankTypeID id)
			throws JsonMappingException, JsonProcessingException, IOException {
		String userID = save.getSaveID();
		if (id == RankTypeID.CLAN) {
			// TODO: clan XP
			return null;
		} else if (id == RankTypeID.DRAGON) {
			// Find active dragon
			AccountKvDataContainer data = save.getSaveData();

			// Pull dragons
			data = data.getChildContainer("dragons");
			JsonArray dragonIds = new JsonArray();
			if (data.entryExists("dragonlist"))
				dragonIds = data.getEntry("dragonlist").getAsJsonArray();
			else
				data.setEntry("dragonlist", dragonIds);

			// Find dragon
			for (JsonElement ele : dragonIds) {
				String did = ele.getAsString();
				DragonData dragon = new XmlMapper().readValue(data.getEntry("dragon-" + did).getAsString(),
						DragonData.class);

				// Check if active
				if (dragon.isSelected) {
					// Found dragon
					userID = dragon.entityID;
					break;
				}
			}
		}
		return userID;
	}

}
