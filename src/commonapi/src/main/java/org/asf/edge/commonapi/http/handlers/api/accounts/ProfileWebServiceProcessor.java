package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.auth.UserInfoData;
import org.asf.edge.commonapi.xmls.data.ProfileData;
import org.asf.edge.commonapi.xmls.data.ProfileData.AnswerBlock;
import org.asf.edge.commonapi.xmls.data.ProfileData.AnswerBlock.AnswerDataBlock;
import org.asf.edge.commonapi.xmls.data.ProfileData.AvatarBlock;
import org.asf.edge.commonapi.xmls.data.ProfileData.AvatarBlock.AchievementBlock;
import org.asf.edge.commonapi.xmls.data.ProfileData.AvatarBlock.RewardMultiplierBlock;
import org.asf.edge.commonapi.xmls.data.ProfileData.AvatarBlock.SubscriptionBlock;
import org.asf.edge.commonapi.xmls.data.ProfileDataList;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ProfileWebServiceProcessor extends BaseApiHandler<EdgeCommonApiServer> {

	private static AccountManager manager;

	public ProfileWebServiceProcessor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ProfileWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ProfileWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public void getQuestions(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle user profile request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find account
		AccountObject account = manager.getAccount(tkn.accountID);
		if (account == null || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("questiondata.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		setResponseContent("text/xml", data);

		// TODO: filter
	}

	@Function(allowedMethods = { "POST" })
	public void getUserProfile(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle user profile request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find account
		AccountObject account = manager.getAccount(tkn.accountID);
		if (account == null || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find save
		setResponseContent("text/xml",
				req.generateXmlValue("UserProfileDisplayData", getProfile(tkn.saveID, account, req, false)));
	}

	@Function(allowedMethods = { "POST" })
	public void getUserProfileByUserID(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle user profile request
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
		AccountSaveContainer save = account.getSave(req.payload.get("userId"));
		if (save == null) {
			// Not part of the user
			// Find profile by ID
			save = manager.getSaveByID(req.payload.get("userId"));
			if (save == null) {
				// Error
				setResponseStatus(404, "Not found");
				return;
			}

			// Found it
			setResponseContent("text/xml", req.generateXmlValue("UserProfileDisplayData",
					getProfile(save.getSaveID(), save.getAccount(), req, true)));
			return;
		}

		// Set response
		setResponseContent("text/xml",
				req.generateXmlValue("UserProfileDisplayData", getProfile(save.getSaveID(), account, req, false)));
	}

	@Function(allowedMethods = { "POST" })
	public void getDetailedChildList(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle user profile request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("parentApiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Find account
		AccountObject account = manager.getAccount(tkn.accountID);
		if (account == null) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Build list
		ProfileDataList lst = new ProfileDataList();
		lst.profiles = Stream.of(account.getSaveIDs()).map(id -> {
			try {
				return getProfile(id, account, req, false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).toArray(t -> new ProfileData[t]);
		if (lst.profiles.length != 0)
			setResponseContent("text/xml", req.generateXmlValue("ArrayOfUserProfileDisplayData", lst));
		else
			setResponseContent("text/xml", req.generateXmlValue("ArrayOfUserProfileDisplayData", null));
	}

	private ProfileData getProfile(String saveID, AccountObject account, ServiceRequestInfo req, boolean minimal)
			throws IOException {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		AccountSaveContainer save = account.getSave(saveID);
		AccountDataContainer data = save.getSaveData();

		// Build profile container
		ProfileData profile = new ProfileData();

		// Populate profile block data
		profile.avatar = new AvatarBlock();
		profile.id = save.getSaveID();

		// Load avatar from save
		if (data.entryExists("avatar")) {
			// Load data
			profile.avatar.avatarData = req.parseXmlValue(data.getEntry("avatar").getAsString(), ObjectNode.class);
		}

		// Add save info
		UserInfoData info = new UserInfoData();
		info.userID = save.getSaveID();
		if (!minimal)
			info.parentUserID = account.getAccountID();
		info.username = save.getUsername();
		info.chatEnabled = account.isChatEnabled();
		info.multiplayerEnabled = account.isMultiplayerEnabled();
		info.registrationDate = fmt.format(new Date(account.getRegistrationTimestamp()));
		info.creationDate = fmt.format(new Date(save.getCreationTime()));
		profile.avatar.userInfo = info;

		// Load currency
		if (!minimal) {
			AccountDataContainer currency = data.getChildContainer("currency");
			AccountDataContainer currencyAccWide = account.getAccountData().getChildContainer("currency");
			if (!currencyAccWide.entryExists("gems"))
				currencyAccWide.setEntry("gems", new JsonPrimitive(0));
			if (!currency.entryExists("coins"))
				currency.setEntry("coins", new JsonPrimitive(300));
			profile.gemCount = currencyAccWide.getEntry("gems").getAsInt();
			profile.coinCount = currency.getEntry("coins").getAsInt();
		}

		// Load reward multipliers
		if (!minimal) {
			ArrayList<RewardMultiplierBlock> multipliers = new ArrayList<RewardMultiplierBlock>();
			AccountDataContainer rewardMultipliers = data.getChildContainer("reward_multipliers");
			if (!rewardMultipliers.entryExists("active"))
				rewardMultipliers.setEntry("active", new JsonArray());
			for (JsonElement ele : rewardMultipliers.getEntry("active").getAsJsonArray()) {
				JsonObject multiplierInfo = ele.getAsJsonObject();

				// Add
				RewardMultiplierBlock multiplier = new RewardMultiplierBlock();
				multiplier.expiryTime = fmt.format(new Date(multiplierInfo.get("expiry").getAsLong()));
				multiplier.multiplierFactor = multiplierInfo.get("factor").getAsInt();
				multiplier.typeID = multiplierInfo.get("typeID").getAsInt();
				multipliers.add(multiplier);
			}
			profile.avatar.rewardMultipliers = multipliers.toArray(t -> new RewardMultiplierBlock[t]);
		}

		// Create default subscription block
		if (!minimal) {
			profile.avatar.subscription = new SubscriptionBlock();
			profile.avatar.subscription.accountID = account.getAccountID();
		}

		// Create default achievement block
		profile.avatar.achievements = new AchievementBlock[1];
		profile.avatar.achievements[0] = new AchievementBlock();
		profile.avatar.achievements[0].saveID = save.getSaveID();
		profile.avatar.achievements[0].rankID = 1;
		profile.avatar.achievements[0].pointTypeID = 1;
		profile.avatar.achievementInfo = new AchievementBlock();
		profile.avatar.achievementInfo.saveID = save.getSaveID();
		profile.avatar.achievementInfo.rankID = 1;
		profile.avatar.achievementInfo.pointTypeID = 1;

		// Create answer data
		if (!minimal) {
			profile.answerData = new AnswerBlock();
			profile.answerData.userID = save.getSaveID();

			// Load answer data
			if (!data.entryExists("answerdata"))
				data.setEntry("answerdata", new JsonObject());
			JsonObject answerMap = data.getEntry("answerdata").getAsJsonObject();
			profile.answerData.answers = new AnswerDataBlock[answerMap.size()];
			int i = 0;
			for (String key : answerMap.keySet()) {
				AnswerDataBlock a = new AnswerDataBlock();
				a.answerID = answerMap.get(key).getAsInt();
				a.questionID = Integer.parseInt(key);
				profile.answerData.answers[i++] = a;
			}
		}

		// TODO
		// profile.avatar.achievements
		// profile.avatar.subscription
		// profile.activityCount
		// profile.achievementCount

		return profile;
	}

}
