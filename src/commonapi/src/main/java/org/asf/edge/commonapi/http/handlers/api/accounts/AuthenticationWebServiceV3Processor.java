package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.xmls.ProductRuleData;

public class AuthenticationWebServiceV3Processor extends BaseApiHandler<EdgeCommonApiServer> {

	public AuthenticationWebServiceV3Processor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AuthenticationWebServiceV3Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v3/AuthenticationWebService.asmx";
	}

	@Override
	public String[] allowedMethods() {
		return new String[] { "GET", "POST" }; // FIXME: verify allowed methods
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function
	public void loginParent(FunctionInfo func) throws IOException {
		// Handle login request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Verify login
		if (req.payload.containsKey("childUserID")) {
			// ?????
			long ticks = Long.parseLong(req.payload.get("ticks"));
			String parentApiToken = req.payload.get("parentApiToken");
			String childUserID = req.getEncryptedValue("childUserID");
			String locale = req.payload.get("locale");
			req = req;
			setResponseStatus(404, "Not found");
		} else {
			// User/password initial login
			String parentLoginData = req.getEncryptedValue("parentLoginData");
			req = req;
			setResponseStatus(404, "Not found");
		}
	}

	@Function
	public void getRules(FunctionInfo func) throws IOException {
		// Handle rules request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Build rule payload
		ProductRuleData data = new ProductRuleData();
		data.globalKey = "Edge does not use the hash validation system as its overly complex and TLS is more than enough";
		data.productRules = new ProductRuleData.ProductRulesBlock();
		data.productRules.sslRules = new ProductRuleData.ProductRulesBlock.RuleBlock[0];
		data.productRules.responseHashValidationRules = new ProductRuleData.ProductRulesBlock.RuleBlock[0];

		// Encode to XML
		String xml = req.generateXmlValue("getProductRulesResponse", data);
		String encrypted = req.generateEncryptedResponse(xml);

		// Set response
		setResponseContent("text/xml", encrypted);
	}

}
