package org.asf.edge.gameplayapi.http.handlers.achievements;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.Date;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.entities.achivements.RankMultiplierInfo;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.common.webservices.SodRequestInfo;
import org.asf.edge.common.webservices.annotations.*;
import org.asf.edge.common.xmls.achievements.AchievementInfoList;
import org.asf.edge.common.xmls.achievements.UserRankData;
import org.asf.edge.common.xmls.achievements.UserRankList;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementDragonIdList;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementRewardList;
import org.asf.edge.gameplayapi.xmls.multipliers.RewardTypeMultiplierData;
import org.asf.edge.gameplayapi.xmls.multipliers.RewardTypeMultiplierListData;
import org.asf.nexus.webservices.WebServiceContext;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.FunctionResult;
import org.asf.nexus.webservices.functions.annotations.ApiHandler;
import org.asf.nexus.webservices.functions.annotations.ExperimentalFeature;
import org.asf.nexus.webservices.functions.annotations.Function;
import org.asf.nexus.webservices.functions.annotations.RequestParam;
import org.asf.edge.common.experiments.EdgeDefaultExperiments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class AchievementWebServiceV1Processor extends EdgeWebService<EdgeGameplayApiServer> {

	private static AchievementManager achievementManager;

	public AchievementWebServiceV1Processor(WebServiceContext<EdgeGameplayApiServer> context) {
		super(context);
	}

	@Override
	public EdgeWebService<EdgeGameplayApiServer> createNewInstance(WebServiceContext<EdgeGameplayApiServer> context) {
		return new AchievementWebServiceV1Processor(context);
	}

	@Override
	public String path() {
		return "/AchievementWebService.asmx";
	}

	//
	// Vanilla
	//

	@ApiHandler
	public FunctionResult getAllRanks(FunctionInfo func, SodRequestInfo req) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Find all ranks
		RankInfo[] ranks = achievementManager.getRankDefinitions();
		UserRankData.UserRankDataWrapper[] defs = new UserRankData.UserRankDataWrapper[ranks.length];
		for (int i = 0; i < ranks.length; i++)
			defs[i] = ranks[i].getRawObject().getWrapped();

		// Create response
		UserRankList lst = new UserRankList();
		lst.ranks = defs;
		return ok("text/xml", req.generateXmlValue("ArrayOfUserRank", lst));
	}

	@ApiHandler
	public FunctionResult getAchievementTaskInfo(FunctionInfo func, SodRequestInfo req,
			@RequestParam int[] achievementTaskIDList) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Load request
		int[] ids = achievementTaskIDList;

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("achievementdata/achievementtasks.xml");
		ObjectNode[] achievementTasks = req.parseXmlValue(new String(strm.readAllBytes(), "UTF-8"), ObjectNode[].class);
		strm.close();

		// Build result
		ArrayList<ObjectNode> tasks = new ArrayList<ObjectNode>();
		for (ObjectNode tsk : achievementTasks) {
			if (ids.length == 0 || IntStream.of(ids).anyMatch(t -> t == tsk.get("AchievementTaskID").asInt())) {
				tasks.add(tsk);
			}
		}
		AchievementTaskInfoList lst = new AchievementTaskInfoList();
		lst.achievementTasks = tasks.toArray(t -> new ObjectNode[t]);
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementTaskInfo", lst));
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	private static class AchievementTaskInfoList {

		@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
		private final String xmlns = "http://api.jumpstart.com/";

		@JsonProperty("AchievementTaskInfo")
		@JacksonXmlElementWrapper(useWrapping = false)
		public ObjectNode[] achievementTasks;

	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	@Function("SetUserAchievementAndGetReward")
	@ExperimentalFeature(value = EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT, isReverse = true)
	public FunctionResult setUserAchievementAndGetRewardDummy(FunctionInfo func, SodRequestInfo req,
			AccountSaveContainer save, @RequestParam int achievementID) throws IOException {
		// Handle task reward request
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementReward", new AchievementRewardList()));
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	@Function("SetAchievementAndGetReward")
	@ExperimentalFeature(value = EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT, isReverse = true)
	public FunctionResult setAchievementAndGetRewardDummy(FunctionInfo func, SodRequestInfo req,
			AccountSaveContainer save, @RequestParam int achievementID) throws IOException {
		return setUserAchievementAndGetRewardDummy(func, req, save, achievementID);
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	@Function("SetAchievementByEntityIDs")
	@ExperimentalFeature(value = EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT, isReverse = true)
	public FunctionResult setAchievementByEntityIDsDummy(FunctionInfo func, SodRequestInfo req,
			AccountSaveContainer save, @RequestParam int achievementID, @RequestParam String groupID,
			@RequestParam AchievementDragonIdList petIDs) throws IOException {
		// Handle task reward request
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementReward", new AchievementRewardList()));
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	@ExperimentalFeature(EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT)
	public FunctionResult setUserAchievementAndGetReward(FunctionInfo func, SodRequestInfo req,
			AccountSaveContainer save, @RequestParam int achievementID) throws IOException {
		// Handle task reward request
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Call manager
		AchievementRewardList lst = new AchievementRewardList();
		lst.rewards = achievementManager.unlockAchievement(save, achievementID);
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementReward", lst));
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	@ExperimentalFeature(EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT)
	public FunctionResult setAchievementAndGetReward(FunctionInfo func, SodRequestInfo req, AccountSaveContainer save,
			@RequestParam int achievementID) throws IOException {
		return setUserAchievementAndGetReward(func, req, save, achievementID);
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	@ExperimentalFeature(EdgeDefaultExperiments.ACHIEVEMENTSV1_SUPPORT)
	public FunctionResult setAchievementByEntityIDs(FunctionInfo func, SodRequestInfo req, AccountSaveContainer save,
			@RequestParam int achievementID, @RequestParam String groupID, @RequestParam AchievementDragonIdList petIDs)
			throws IOException {
		// Handle task reward request
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Call manager
		AchievementRewardList lst = new AchievementRewardList();
		lst.rewards = achievementManager.unlockAchievement(save, achievementID, petIDs.ids);
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementReward", lst));
	}

	@ApiHandler
	@EdgeTokenSecured
	public FunctionResult getPetAchievementsByUserID(FunctionInfo func, SodRequestInfo req, SessionToken tkn,
			AccountObject account, @RequestParam String userId) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Find save
		AccountSaveContainer save = account.getSave(userId);
		if (save == null) {
			// Check token
			if (tkn.saveID != null)
				save = account.getSave(tkn.saveID);
			if (save == null) {
				// Error
				return response(404, "Not found");
			}
		}

		// Prepare response
		AchievementInfoList lst = new AchievementInfoList();
		ArrayList<AchievementInfoList.AchievementBlock> ranks = new ArrayList<AchievementInfoList.AchievementBlock>();

		// Find all dragons
		AccountKvDataContainer data = save.getSaveData().getChildContainer("dragons");
		JsonArray dragonIds = new JsonArray();
		if (data.entryExists("dragonlist"))
			dragonIds = data.getEntry("dragonlist").getAsJsonArray();
		else
			data.setEntry("dragonlist", dragonIds);
		for (JsonElement ele : dragonIds) {
			String did = ele.getAsString();
			ObjectNode dragon = req.parseXmlValue(data.getEntry("dragon-" + did).getAsString(), ObjectNode.class);

			// Get ranks
			for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, dragon.get("eid").asText())) {
				// Check
				if (rank.getTypeID() == RankTypeID.DRAGON) {
					// Check age
					if (dragon.has("gs") && dragon.get("gs").has("n")) {
						String stage = dragon.get("gs").get("n").asText().toUpperCase();
						switch (stage) {

						case "TITAN": {
							// Boost to level 20

							// Add XP if needed
							int level = 1;
							for (RankInfo r : achievementManager.getRankDefinitionsByPointType(8)) {
								if (level == 20) {
									if (rank.getTotalScore() < r.getValue())
										rank.setTotalScore(r.getValue());
									break;
								}
								level++;
							}

							break;
						}

						case "ADULT": {
							// Boost to level 10

							// Add XP if needed
							int level = 1;
							for (RankInfo r : achievementManager.getRankDefinitionsByPointType(8)) {
								if (level == 10) {
									if (rank.getTotalScore() < r.getValue())
										rank.setTotalScore(r.getValue());
									break;
								}
								level++;
							}

							break;
						}

						}
					}
				}

				// Add rank
				AchievementInfoList.AchievementBlock r = new AchievementInfoList.AchievementBlock();
				r.userID = new AchievementInfoList.AchievementBlock.StringWrapper(rank.getEntityID());
				r.pointTypeID = new AchievementInfoList.AchievementBlock.IntWrapper(rank.getTypeID().getPointTypeID());
				r.pointsTotal = new AchievementInfoList.AchievementBlock.IntWrapper(rank.getTotalScore());
				r.rank = new AchievementInfoList.AchievementBlock.IntWrapper(
						AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
				ranks.add(r);
			}
		}

		// Set result
		lst.achievements = ranks.toArray(t -> new AchievementInfoList.AchievementBlock[t]);

		// Set response
		return ok("text/xml", req.generateXmlValue("ArrayOfUserAchievementInfo", lst));
	}

	@ApiHandler
	@EdgeTokenSecured
	public FunctionResult getAchievementsByUserID(FunctionInfo func, SessionToken tkn, SodRequestInfo req,
			AccountObject account, @RequestParam String userId) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Find save
		AccountSaveContainer save = account.getSave(userId);
		if (save == null) {
			// Check token
			if (tkn.saveID != null)
				save = account.getSave(tkn.saveID);
			if (save == null) {
				// Error
				return response(404, "Not found");
			}
		}

		// Prepare response
		AchievementInfoList lst = new AchievementInfoList();
		ArrayList<AchievementInfoList.AchievementBlock> ranks = new ArrayList<AchievementInfoList.AchievementBlock>();

		// Get ranks
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, userId)) {
			// Add rank
			AchievementInfoList.AchievementBlock r = new AchievementInfoList.AchievementBlock();
			if (userId.equals(save.getSaveID()))
				r.saveName = new AchievementInfoList.AchievementBlock.StringWrapper(save.getUsername());
			r.userID = new AchievementInfoList.AchievementBlock.StringWrapper(rank.getEntityID());
			r.pointTypeID = new AchievementInfoList.AchievementBlock.IntWrapper(rank.getTypeID().getPointTypeID());
			r.pointsTotal = new AchievementInfoList.AchievementBlock.IntWrapper(rank.getTotalScore());
			r.rank = new AchievementInfoList.AchievementBlock.IntWrapper(
					AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
			ranks.add(r);
		}

		// Set result
		lst.achievements = ranks.toArray(t -> new AchievementInfoList.AchievementBlock[t]);

		// Set response
		return ok("text/xml", req.generateXmlValue("ArrayOfUserAchievementInfo", lst));
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	public FunctionResult getUserAchievements(FunctionInfo func, SessionToken tkn, SodRequestInfo req,
			AccountObject account, AccountSaveContainer save) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Prepare response
		AchievementInfoList lst = new AchievementInfoList();
		ArrayList<AchievementInfoList.AchievementBlock> ranks = new ArrayList<AchievementInfoList.AchievementBlock>();

		// Get ranks
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, save.getSaveID())) {
			// Add rank
			AchievementInfoList.AchievementBlock r = new AchievementInfoList.AchievementBlock();
			r.saveName = new AchievementInfoList.AchievementBlock.StringWrapper(save.getUsername());
			r.userID = new AchievementInfoList.AchievementBlock.StringWrapper(rank.getEntityID());
			r.pointTypeID = new AchievementInfoList.AchievementBlock.IntWrapper(rank.getTypeID().getPointTypeID());
			r.pointsTotal = new AchievementInfoList.AchievementBlock.IntWrapper(rank.getTotalScore());
			r.rank = new AchievementInfoList.AchievementBlock.IntWrapper(
					AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
			ranks.add(r);
		}

		// Set result
		lst.achievements = ranks.toArray(t -> new AchievementInfoList.AchievementBlock[t]);

		// Set response
		return ok("text/xml", req.generateXmlValue("ArrayOfUserAchievementInfo", lst));
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	public FunctionResult getAllRewardTypeMultiplier(FunctionInfo func, SessionToken tkn, SodRequestInfo req,
			AccountObject account, AccountSaveContainer save) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Prepare multiplier list
		ArrayList<RewardTypeMultiplierData> multipliers = new ArrayList<RewardTypeMultiplierData>();

		// Find multipliers
		for (RankMultiplierInfo m : AchievementManager.getInstance().getServerwideRankMultipliers()) {
			// Add
			SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt2.setTimeZone(TimeZone.getTimeZone("UTC"));
			RewardTypeMultiplierData data = new RewardTypeMultiplierData();
			data.typeID = m.getPointType().getPointTypeID();
			data.factor = m.getMultiplicationFactor();
			data.expiryTime = fmt2.format(new Date(m.getExpiryTime()));
			multipliers.add(data);
		}

		// Set response
		RewardTypeMultiplierListData resp = new RewardTypeMultiplierListData();
		resp.multipliers = multipliers.toArray(t -> new RewardTypeMultiplierData[t]);
		return ok("text/xml", req.generateXmlValue("ArrayOfRewardTypeMultiplier", resp));
	}

	//
	// Dragonrescue import
	//

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	public FunctionResult setDragonXP(FunctionInfo func, SodRequestInfo req, AccountSaveContainer save,
			@RequestParam String dragonId, @RequestParam int value) throws IOException {
		// Retrieve rank
		EntityRankInfo rank = AchievementManager.getInstance().getRank(save, dragonId, RankTypeID.DRAGON);
		if (rank == null)
			return response(409, "Conflict", "Dragon not found");

		// Assign
		rank.setTotalScore(value);

		// Set response
		return ok("text/plain", "OK");
	}

	@ApiHandler
	@EdgeTokenSecured
	@EdgeTokenRequireSave
	@EdgeTokenRequireCapability("gp")
	public FunctionResult setPlayerXP(AccountSaveContainer save, SodRequestInfo req, @RequestParam int type,
			@RequestParam int value) throws IOException {
		// Retrieve rank
		EntityRankInfo rank = AchievementManager.getInstance().getRank(save, save.getSaveID(),
				RankTypeID.getByTypeID(type));

		// Assign
		rank.setTotalScore(value);

		// Set response
		return ok("text/plain", "OK");
	}

}
