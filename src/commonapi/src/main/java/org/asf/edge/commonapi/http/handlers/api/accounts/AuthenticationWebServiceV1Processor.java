package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
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

public class AuthenticationWebServiceV1Processor extends BaseApiHandler<EdgeCommonApiServer> {

	private AccountManager manager;

	public AuthenticationWebServiceV1Processor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AuthenticationWebServiceV1Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/AuthenticationWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public void isValidApiToken_V2(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle token verification
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);

		// Return result
		switch (res) {

		case SUCCESS:
			// Update token
			tkn.updateSessionTime();
			setResponseContent("text/xml", req.generateXmlValue("ApiTokenStatus", 1));
			break;

		case INVALID_DATA:
			setResponseContent("text/xml", req.generateXmlValue("ApiTokenStatus", 3));
			break;

		case LOGGED_IN_ELSEWHERE:
			setResponseContent("text/xml", req.generateXmlValue("ApiTokenStatus", 4));
			break;

		case TOKEN_EXPIRED:
			setResponseContent("text/xml", req.generateXmlValue("ApiTokenStatus", 2));
			break;
		default:
			break;

		}
	}

	@Function(allowedMethods = { "POST" })
	public void loginChild(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle login request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("parentApiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Read save ID
		String saveID = req.getEncryptedValue("childUserID");

		// Locate save
		AccountSaveContainer save = account.getSave(saveID);
		if (save == null) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Build save-specific token
		tkn = new SessionToken();
		tkn.accountID = account.getAccountID();
		tkn.saveID = saveID;
		tkn.lastLoginTime = account.getLastLoginTime();
		tkn.capabilities = new String[] { "api", "gp" };

		// Set response
		setResponseContent("text/xml", req.generateEncryptedResponse(getUtilities().encodeToken(tkn.toTokenString())));
	}

	@Function(allowedMethods = { "POST" })
	public void getUserInfoByApiToken(FunctionInfo func) throws IOException {
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
		if (account == null) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Check save
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		if (tkn.saveID != null) {
			// Save specific
			AccountSaveContainer save = account.getSave(tkn.saveID);
			if (save == null) {
				// Error
				setResponseStatus(404, "Not found");
				return;
			}

			// Set response
			UserInfoData info = new UserInfoData();
			info.userID = save.getSaveID();
			info.parentUserID = account.getAccountID();
			info.username = save.getUsername();
			info.chatEnabled = account.isChatEnabled();
			info.multiplayerEnabled = account.isMultiplayerEnabled();
			info.registrationDate = fmt.format(new Date(account.getRegistrationTimestamp()));
			info.creationDate = fmt.format(new Date(save.getCreationTime()));
			setResponseContent("text/xml", req.generateXmlValue("UserInfo", info));
		} else {
			// Account

			// Set response
			UserInfoData info = new UserInfoData();
			info.userID = account.getAccountID();
			info.parentUserID = account.getAccountID();
			info.username = account.getUsername();
			info.chatEnabled = account.isChatEnabled();
			info.multiplayerEnabled = account.isMultiplayerEnabled();
			info.registrationDate = fmt.format(new Date(account.getRegistrationTimestamp()));
			info.creationDate = fmt.format(new Date(account.getRegistrationTimestamp()));
			setResponseContent("text/xml", req.generateXmlValue("UserInfo", info));
		}
	}

}
