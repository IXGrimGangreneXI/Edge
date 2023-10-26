package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.EdgeWebService;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;

public class MobileStoreWebServiceProcessor extends EdgeWebService<EdgeGameplayApiServer> {

	public MobileStoreWebServiceProcessor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new MobileStoreWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/MobileStoreWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

}
