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
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankMultiplierInfo;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.util.AvatarDowngrader;
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
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ProfileWebServiceProcessor extends EdgeWebService<EdgeCommonApiServer> {

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

	@LegacyFunction(allowedMethods = { "POST" })
	public void getProfileTagAll(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle profile tag request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// TODO: dummy'd
		setResponseContent("text/xml", req.generateXmlValue("ArrayOfProfileTag", null));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getQuestions(LegacyFunctionInfo func) throws IOException {
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
		data = data.replace("http://media.jumpstart.com/", "RS_DATA/");
		data = data.replace("https://media.jumpstart.com/", "RS_DATA/");
		data = data.replace("http://media.schoolofdragons.com/", "RS_DATA/");
		data = data.replace("https://media.schoolofdragons.com/", "RS_DATA/");
		setResponseContent("text/xml", data);

		// TODO: filter
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getUserProfile(LegacyFunctionInfo func) throws IOException {
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

		// Find profile
		ProfileData profile = getProfile(tkn.saveID, account, req, false);

		// Check filter
		if (TextFilterService.getInstance().isFiltered(account.getSave(profile.id).getUsername(), true)) {
			// Filtered
			if (profile.avatar.avatarData.has("DisplayName"))
				profile.avatar.avatarData.remove("DisplayName");
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("UserProfileDisplayData", profile));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getUserProfileByUserID(LegacyFunctionInfo func) throws IOException {
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

			// Find profile
			ProfileData profile = getProfile(save.getSaveID(), save.getAccount(), req, true);

			// Check filter
			if (TextFilterService.getInstance().isFiltered(save.getUsername(), true)) {
				// Filtered
				if (profile.avatar.avatarData.has("DisplayName"))
					profile.avatar.avatarData.set("DisplayName", new TextNode(TextFilterService.getInstance()
							.filterString(save.getUsername(), account.isStrictChatFilterEnabled())));
			}

			// Set response
			setResponseContent("text/xml", req.generateXmlValue("UserProfileDisplayData", profile));
			return;
		}

		// Find profile
		ProfileData profile = getProfile(save.getSaveID(), account, req, false);

		// Check filter
		if (TextFilterService.getInstance().isFiltered(account.getSave(profile.id).getUsername(), true)) {
			// Filtered
			if (profile.avatar.avatarData.has("DisplayName"))
				profile.avatar.avatarData.remove("DisplayName");
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("UserProfileDisplayData", profile));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getDetailedChildList(LegacyFunctionInfo func) throws IOException {
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
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
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

			// Downgrade avatar
			profile.avatar.avatarData = AvatarDowngrader.downgradeAvatar(profile.avatar.avatarData, account, save, 6, 0,
					0);
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

		// Load reward multipliers if needed
		if (!minimal) {
			// Load reward multipliers
			ArrayList<RewardMultiplierBlock> multipliers = new ArrayList<RewardMultiplierBlock>();
			for (RankMultiplierInfo m : AchievementManager.getInstance().getUserRankMultipliers(save)) {
				// Add
				RewardMultiplierBlock multiplier = new RewardMultiplierBlock();
				multiplier.expiryTime = fmt.format(new Date(m.getExpiryTime()));
				multiplier.multiplierFactor = m.getMultiplicationFactor();
				multiplier.typeID = m.getPointType().getPointTypeID();
				multipliers.add(multiplier);
			}
			profile.avatar.rewardMultipliers = multipliers.toArray(t -> new RewardMultiplierBlock[t]);
		}

		// Create default subscription block
		if (!minimal) {
			profile.avatar.subscription = new SubscriptionBlock();
			profile.avatar.subscription.accountID = account.getAccountID();
		}

		// Load ranks
		ArrayList<AchievementBlock> ranks = new ArrayList<AchievementBlock>();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, saveID)) {
			AchievementBlock block = new AchievementBlock();
			block.saveID = rank.getEntityID();
			block.pointsTotal = rank.getTotalScore();
			block.pointTypeID = rank.getTypeID().getPointTypeID();
			block.rank = AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1;
			ranks.add(block);
		}
		profile.avatar.achievements = ranks.toArray(t -> new AchievementBlock[t]);

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

		// Check filter
		if (TextFilterService.getInstance().isFiltered(save.getUsername(), true)) {
			// Force name change
			if (profile.avatar.avatarData != null && profile.avatar.avatarData.has("DisplayName"))
				profile.avatar.avatarData.set("DisplayName", new TextNode("Viking-" + save.getSaveID()));
		}

		// TODO
		// profile.avatar.achievements
		// profile.avatar.subscription
		// profile.activityCount
		// profile.achievementCount

		return profile;
	}

}
