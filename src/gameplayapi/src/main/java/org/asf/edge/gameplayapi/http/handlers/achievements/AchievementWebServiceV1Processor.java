package org.asf.edge.gameplayapi.http.handlers.achievements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.entities.achivements.RankInfo;
import org.asf.edge.gameplayapi.services.achievements.AchievementManager;
import org.asf.edge.gameplayapi.xmls.achievements.EmptyAchievementInfoList;
import org.asf.edge.gameplayapi.xmls.achievements.UserRankData;
import org.asf.edge.gameplayapi.xmls.achievements.UserRankList;
import org.asf.edge.gameplayapi.xmls.multipliers.RewardTypeMultiplierData;
import org.asf.edge.gameplayapi.xmls.multipliers.RewardTypeMultiplierListData;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAllRanks(LegacyFunctionInfo func) throws IOException {
		if (achievementManager == null)
			achievementManager = AchievementManager.getInstance();

		// Handle rank request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Find all ranks
		RankInfo[] ranks = achievementManager.getRankDefinitions();
		UserRankData.UserRankDataWrapper[] defs = new UserRankData.UserRankDataWrapper[ranks.length];
		for (int i = 0; i < ranks.length; i++)
			defs[i] = ranks[i].getRawObject().getWrapped();

		// Create response
		UserRankList lst = new UserRankList();
		lst.ranks = defs;
		setResponseContent("text/xml", req.generateXmlValue("ArrayOfUserRank", lst));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAchievementTaskInfo(LegacyFunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// FIXME: implement properly
		// Needs filtering

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("achievementdata/achievementtasks.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		setResponseContent("text/xml", data);
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getPetAchievementsByUserID(LegacyFunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find save
		String id = req.payload.get("userId");
		AccountDataContainer saveData = account.getAccountData();
		if (!id.equals(account.getAccountID()))
			saveData = account.getSave(id).getSaveData();

		// FIXME: implement properly

		// Set response
		setResponseContent("text/xml",
				req.generateXmlValue("ArrayOfUserAchievementInfo", new EmptyAchievementInfoList()));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAchievementsByUserID(LegacyFunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find save
		String id = req.payload.get("userId");
		// NOTE: the user id may be a dragon

		// FIXME: implement properly

		// Set response
		setResponseContent("text/xml",
				req.generateXmlValue("ArrayOfUserAchievementInfo", new EmptyAchievementInfoList()));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAllRewardTypeMultiplier(LegacyFunctionInfo func) throws IOException, ParseException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);
		if (save == null) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Prepare multiplier list
		ArrayList<RewardTypeMultiplierData> multipliers = new ArrayList<RewardTypeMultiplierData>();

		// Load multipliers from disk
		File multiplierConfig = new File("multipliers.json");
		SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		if (!multiplierConfig.exists()) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR, 12);
			cal.set(Calendar.MINUTE, 00);
			cal.add(Calendar.DAY_OF_MONTH, 10);
			Date t = cal.getTime();

			// Generate
			Files.writeString(multiplierConfig.toPath(), "{\n"
					+ "    \"__COMMENT__\": \"This file defines the active multipliers used in the game.\",\n"
					+ "    \"__COMMENT2_\": \"The example below is generated at first run and is configured to create 3 multipliers that are valid for 10 days.\",\n"
					+ "\n" //
					+ "    \"multipliers\": [\n" //
					+ "        {\n" //
					+ "            \"type\": 1,\n" //
					+ "            \"factor\": 2,\n" //
					+ "            \"expiry\": \"" + fmt.format(t) + "\"\n" //
					+ "        },\n" //
					+ "        {\n" //
					+ "            \"type\": 12,\n" //
					+ "            \"factor\": 2,\n" //
					+ "            \"expiry\": \"" + fmt.format(t) + "\"\n" //
					+ "        },\n"//
					+ "        {\n" //
					+ "            \"type\": 8,\n" //
					+ "            \"factor\": 2,\n" //
					+ "            \"expiry\": \"" + fmt.format(t) + "\"\n" //
					+ "        }\n" //
					+ "    ]\n" //
					+ "}\n");
		}

		// Read config
		JsonObject conf = JsonParser.parseString(Files.readString(multiplierConfig.toPath())).getAsJsonObject();
		for (JsonElement ele : conf.get("multipliers").getAsJsonArray()) {
			JsonObject m = ele.getAsJsonObject();

			// Parse time
			Date t = fmt.parse(m.get("expiry").getAsString());
			SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
			fmt2.setTimeZone(TimeZone.getTimeZone("UTC"));

			// Add
			RewardTypeMultiplierData data = new RewardTypeMultiplierData();
			data.typeID = m.get("type").getAsInt();
			data.factor = m.get("factor").getAsInt();
			data.expiryTime = fmt2.format(t);
			multipliers.add(data);
		}

		// Set response
		RewardTypeMultiplierListData resp = new RewardTypeMultiplierListData();
		resp.multipliers = multipliers.toArray(t -> new RewardTypeMultiplierData[t]);
		setResponseContent("text/xml", req.generateXmlValue("ArrayOfRewardTypeMultiplier", resp));
	}

}
