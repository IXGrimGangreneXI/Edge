package org.asf.edge.gameplayapi.http.handlers.achievements;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskData;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskSetRequestList;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskSetResponseData;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementTaskSetResponseList;

public class AchievementWebServiceV2Processor extends EdgeWebService<EdgeGameplayApiServer> {

	public AchievementWebServiceV2Processor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AchievementWebServiceV2Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v2/AchievementWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult setUserAchievementTask(FunctionInfo func) throws IOException {
		// Handle task reward request
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

		// Parse request
		String request = req.getEncryptedValue("achievementTaskSetRequest");
		AchievementTaskSetRequestList lst = req.parseXmlValue(request, AchievementTaskSetRequestList.class);

		// Prepare response
		// TODO: stubbed
		ArrayList<AchievementTaskSetResponseData> responses = new ArrayList<AchievementTaskSetResponseData>();
		for (AchievementTaskData reqA : lst.requests) {
			// TODO: stubbed
			AchievementTaskSetResponseData resp = new AchievementTaskSetResponseData();
			resp.success = false;
			responses.add(resp);
		}

		// Create response
		AchievementTaskSetResponseList resp = new AchievementTaskSetResponseList();
		resp.responses = responses.toArray(t -> new AchievementTaskSetResponseData[t]);
		return ok("text/xml", req.generateXmlValue("ArrayOfAchievementTaskSetResponse", resp));
	}

}
