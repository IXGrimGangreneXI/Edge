package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class AuthenticationWebServiceV1Processor extends BaseApiHandler<EdgeCommonApiServer> {

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
	public String[] allowedMethods() {
		return new String[] { "GET", "POST" }; // FIXME: verify allowed methods
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

}
