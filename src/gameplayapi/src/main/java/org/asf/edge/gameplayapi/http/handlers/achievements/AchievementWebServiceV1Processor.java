package org.asf.edge.gameplayapi.http.handlers.achievements;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Date;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.entities.achivements.RankMultiplierInfo;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.xmls.achievements.AchievementInfoList;
import org.asf.edge.common.xmls.achievements.UserRankData;
import org.asf.edge.common.xmls.achievements.UserRankList;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.xmls.multipliers.RewardTypeMultiplierData;
import org.asf.edge.gameplayapi.xmls.multipliers.RewardTypeMultiplierListData;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class AchievementWebServiceV1Processor extends EdgeWebService<EdgeGameplayApiServer> {

	private static AchievementManager achievementManager;

	public AchievementWebServiceV1Processor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AchievementWebServiceV1Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/AchievementWebService.asmx";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getAllRanks(FunctionInfo func) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Handle rank request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");

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

	@Function(allowedMethods = { "POST" })
	public FunctionResult getAchievementTaskInfo(FunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");

		// FIXME: implement properly
		// Needs filtering

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("achievementdata/achievementtasks.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		return ok("text/xml", data);
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getPetAchievementsByUserID(FunctionInfo func) throws IOException {
		// Handle dragon ranks request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		String id = req.payload.get("userId");
		AccountSaveContainer save = account.getSave(id);
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
		AccountDataContainer data = save.getSaveData().getChildContainer("dragons");
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

	@Function(allowedMethods = { "POST" })
	public FunctionResult getAchievementsByUserID(FunctionInfo func) throws IOException {
		// Handle ranks request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		String id = req.payload.get("userId");
		AccountSaveContainer save = account.getSave(id);
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
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, id)) {
			// Add rank
			AchievementInfoList.AchievementBlock r = new AchievementInfoList.AchievementBlock();
			if (id.equals(save.getSaveID()))
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

	@Function(allowedMethods = { "POST" })
	public FunctionResult getUserAchievements(FunctionInfo func) throws IOException {
		// Handle ranks request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);

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

	@Function(allowedMethods = { "POST" })
	public FunctionResult getAllRewardTypeMultiplier(FunctionInfo func) throws IOException, ParseException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);
		if (save == null) {
			// Error
			return response(404, "Not found");
		}

		// Prepare multiplier list
		ArrayList<RewardTypeMultiplierData> multipliers = new ArrayList<RewardTypeMultiplierData>();

		// Find multipliers
		for (RankMultiplierInfo m : AchievementManager.getInstance().getServerwideRankMultipliers()) {
			// Add
			SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
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

}
