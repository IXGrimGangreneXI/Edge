package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.account.AccountObject;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.auth.ChildAvatarRegistrationData;
import org.asf.edge.commonapi.xmls.auth.LoginStatusType;
import org.asf.edge.commonapi.xmls.auth.RegistrationResultData;
import org.asf.edge.commonapi.xmls.auth.RegistrationResultData.SuggestionResultBlock;

public class RegistrationWebServiceV4Processor extends BaseApiHandler<EdgeCommonApiServer> {

	private AccountManager manager;

	public RegistrationWebServiceV4Processor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new RegistrationWebServiceV4Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v4/RegistrationWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public void registerChild(FunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Handle name validation request
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

		// Decode registration data
		ChildAvatarRegistrationData reg = req.parseXmlValue(req.getEncryptedValue("childRegistrationData"),
				ChildAvatarRegistrationData.class);

		// Check validity
		if (!manager.isValidUsername(reg.name)) {
			// Invalid name
			RegistrationResultData resp = new RegistrationResultData();
			resp.suggestions = null;
			resp.status = LoginStatusType.InvalidUserName;
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Check filters
		// FIXME: implement this, use the same error response as invalid names for this

		// Check if in use
		boolean inUse = false;
		if (!account.getUsername().equalsIgnoreCase(reg.name) && manager.isUsernameTaken(reg.name)) {
			inUse = true;
		} else {
			// Check if in use by any saves
			if (Stream.of(account.getSaveIDs()).map(t -> account.getSave(t)).anyMatch(t -> {
				try {
					return t.getUsername().equalsIgnoreCase(reg.name) && t.getSaveData().entryExists("avatar");
				} catch (IOException e) {
					return false;
				}
			})) {
				inUse = true;
			}
		}
		if (inUse) {
			// Taken
			RegistrationResultData resp = new RegistrationResultData();
			resp.suggestions = null;
			resp.status = LoginStatusType.InvalidUserName;
			resp.suggestions = new SuggestionResultBlock();

			// Generate suggestions
			// TODO: better suggestions
			Random rnd = new Random();
			ArrayList<String> suggestions = new ArrayList<String>();
			for (int i = 1000; i < 9999; i++) {
				if (suggestions.size() == 6)
					break;
				if (!manager.isUsernameTaken(reg.name + rnd.nextInt(1000, 9999)))
					suggestions.add(reg.name + rnd.nextInt(1000, 9999));
			}

			// Set response
			resp.suggestions.suggestions = suggestions.toArray(t -> new String[t]);
			setResponseContent("text/xml",
					req.generateEncryptedResponse(req.generateXmlValue("RegistrationResult", resp)));
			return;
		}

		// Validation success
		setResponseStatus(404, "Not found");
	}
}
