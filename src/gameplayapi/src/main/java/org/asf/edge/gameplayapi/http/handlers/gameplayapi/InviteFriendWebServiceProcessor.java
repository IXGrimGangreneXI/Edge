package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;

public class InviteFriendWebServiceProcessor extends EdgeWebService<EdgeGameplayApiServer> {

	public InviteFriendWebServiceProcessor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new InviteFriendWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v2/InviteFriendWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

}
