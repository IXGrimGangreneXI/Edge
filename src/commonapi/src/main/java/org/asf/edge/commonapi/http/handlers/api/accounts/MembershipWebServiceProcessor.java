package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.EdgeWebService;
import org.asf.edge.common.http.functions.LegacyFunction;
import org.asf.edge.common.http.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.commonapi.EdgeCommonApiServer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class MembershipWebServiceProcessor extends EdgeWebService<EdgeCommonApiServer> {

	public MembershipWebServiceProcessor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new MembershipWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/MembershipWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getChildList(LegacyFunctionInfo func) throws IOException {
		AccountManager manager = AccountManager.getInstance();

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

		// Build list
		String[] lst = Stream.of(account.getSaveIDs()).map(t -> t + ", " + account.getSave(t).getUsername())
				.toArray(t -> new String[t]);
		StringList ls = new StringList();
		ls.strings = lst;
		if (lst.length != 0)
			setResponseContent("text/xml", req.generateXmlValue("ArrayOfString", ls));
		else
			setResponseContent("text/xml", req.generateXmlValue("ArrayOfString", null));
	}

	private static class StringList {

		@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
		private final String xmlns = "http://api.jumpstart.com/";
		
		@JsonProperty("string")
		@JacksonXmlElementWrapper(useWrapping = false)
		public String[] strings;

	}

}
