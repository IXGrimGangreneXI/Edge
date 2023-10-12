package org.asf.edge.modules.gridapi.phoenixconnector;

import java.util.function.Consumer;

import org.asf.connective.RemoteClient;

import com.google.gson.JsonObject;

public interface IApiConnectorRequestHandler {

	/**
	 * Defines the request type
	 * 
	 * @return Request type string
	 */
	public String requestType();

	/**
	 * Called to handle the request
	 * 
	 * @param payload          Request payload
	 * @param client           Requesting client
	 * @param responseCallback Response callback
	 */
	public void handle(JsonObject payload, RemoteClient client, Consumer<JsonObject> responseCallback);

}
