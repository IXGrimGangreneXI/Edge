package org.asf.edge.commonapi.http.handlers.api.messaging;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class ChatWebServiceProcessor extends EdgeWebService<EdgeCommonApiServer> {

	public ChatWebServiceProcessor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ChatWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ChatWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

}
