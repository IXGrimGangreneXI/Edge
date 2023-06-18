package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.account.AccountObject;
import org.asf.edge.common.account.AccountSaveContainer;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.auth.UserInfoData;
import org.asf.edge.commonapi.xmls.data.ProfileData;
import org.asf.edge.commonapi.xmls.data.ProfileData.AvatarBlock;
import org.asf.edge.commonapi.xmls.data.ProfileData.AvatarBlock.RewardMultiplierBlock;
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
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
		lst.profiles = Stream.of(account.getSaveIDs()).map(id -> {
			try {
				AccountSaveContainer save = account.getSave(id);
				AccountDataContainer data = save.getSaveData();

				// Build profile container
				ProfileData profile = new ProfileData();

				// Populate profile block data
				profile.avatar = new AvatarBlock();
				profile.id = save.getSaveID();

				// Load avatar from save
				if (data.entryExists("avatar")) {
					// Load data
					profile.avatar.avatarData = req.parseXmlValue(data.getEntry("avatar").getAsString(),
							ObjectNode.class);
				}

				// Add save info
				UserInfoData info = new UserInfoData();
				info.userID = save.getSaveID();
				info.parentUserID = account.getAccountID();
				info.username = save.getUsername();
				info.chatEnabled = account.isChatEnabled();
				info.multiplayerEnabled = account.isMultiplayerEnabled();
				info.registrationDate = fmt.format(new Date(account.getRegistrationTimestamp()));
				info.creationDate = fmt.format(new Date(save.getCreationTime()));
				profile.avatar.userInfo = info;

				// Load currency
				AccountDataContainer currency = data.getChildContainer("currency");
				if (!currency.entryExists("gems"))
					currency.setEntry("gems", new JsonPrimitive(75));
				if (!currency.entryExists("coins"))
					currency.setEntry("coins", new JsonPrimitive(300));
				profile.gemCount = currency.getEntry("gems").getAsInt();
				profile.coinCount = currency.getEntry("coins").getAsInt();

				// Load reward multipliers
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

				// TODO
				// profile.avatar.achievements
				// profile.avatar.subscription
				// profile.activityCount
				// profile.achievementCount
				// profile.mythieCount

				return profile;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).toArray(t -> new ProfileData[t]);
		setResponseContent("text/xml", req.generateXmlValue("ArrayOfUserProfileDisplayData", lst));
	}

}
