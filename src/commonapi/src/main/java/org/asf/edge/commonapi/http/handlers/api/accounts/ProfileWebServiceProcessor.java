package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class ProfileWebServiceProcessor extends BaseApiHandler<EdgeCommonApiServer> {

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

}
