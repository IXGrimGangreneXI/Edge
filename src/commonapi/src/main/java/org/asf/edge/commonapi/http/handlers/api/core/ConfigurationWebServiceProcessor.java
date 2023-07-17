package org.asf.edge.commonapi.http.handlers.api.core;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class ConfigurationWebServiceProcessor extends EdgeWebService<EdgeCommonApiServer> {

	public ConfigurationWebServiceProcessor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ConfigurationWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ConfigurationWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getMMOServerInfoWithZone(LegacyFunctionInfo info) throws IOException {
		// Handle server list request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Dummy
		setResponseContent("text/xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<ArrayOfMMOServerInfo xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://api.jumpstart.com/\" />");
	}
}
