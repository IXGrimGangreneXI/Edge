package org.asf.edge.gameplayapi.http.handlers.achievements;

import java.io.IOException;
import java.io.InputStream;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountObject;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.xmls.achievements.EmptyAchievementInfoList;

public class AchievementWebServiceV1Processor extends BaseApiHandler<EdgeGameplayApiServer> {

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

	@Function(allowedMethods = { "POST" })
	public void getAllRanks(FunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// FIXME: implement properly

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("ranks.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		setResponseContent("text/xml", data);
	}

	@Function(allowedMethods = { "POST" })
	public void getAchievementTaskInfo(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getPetAchievementsByUserID(FunctionInfo func) throws IOException {
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

}
