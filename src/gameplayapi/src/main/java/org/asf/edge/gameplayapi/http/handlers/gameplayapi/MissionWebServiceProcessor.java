package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;

public class MissionWebServiceProcessor extends BaseApiHandler<EdgeGameplayApiServer> {

	public MissionWebServiceProcessor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new MissionWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/MissionWebService.asmx";
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
