package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class RegistrationWebServiceV4Processor extends BaseApiHandler<EdgeCommonApiServer> {

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

}
