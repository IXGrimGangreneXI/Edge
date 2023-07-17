package org.asf.edge.commonapi.http.handlers.api.messaging;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.commonapi.EdgeCommonApiServer;

public class MessagingWebServiceProcessor extends EdgeWebService<EdgeCommonApiServer> {

	public MessagingWebServiceProcessor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new MessagingWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/MessagingWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function
	public FunctionResult getUserMessageQueue(FunctionInfo func) {
		// TODO: stubbed
		return ok("text/xml",
				"<ArrayOfMessageInfo xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://api.jumpstart.com/\" />");
	}

}
