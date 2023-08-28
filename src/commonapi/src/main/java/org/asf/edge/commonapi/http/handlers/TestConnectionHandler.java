package org.asf.edge.commonapi.http.handlers;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpRequestProcessor;

public class TestConnectionHandler extends HttpRequestProcessor {

	@Override
	public String path() {
		return "/testconnection";
	}

	@Override
	public void process(String path, String method, RemoteClient client) throws IOException {
		setResponseContent("text/plain", "OK");
		setResponseStatus(200, "OK");
	}

	@Override
	public HttpRequestProcessor createNewInstance() {
		return new TestConnectionHandler();
	}

}
