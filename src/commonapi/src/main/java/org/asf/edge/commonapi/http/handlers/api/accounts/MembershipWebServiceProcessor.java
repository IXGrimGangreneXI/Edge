package org.asf.edge.commonapi.http.handlers.api.accounts;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class MembershipWebServiceProcessor extends BaseApiHandler<EdgeCommonApiServer> {

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

}
