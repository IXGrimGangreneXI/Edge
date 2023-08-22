package org.asf.edge.modules.gridclient.phoenix.auth;

import java.util.function.Function;

import com.google.gson.JsonObject;

/**
 * 
 * Login defer message container
 * 
 * @author Sky Swimmer
 *
 */
public class LoginDeferredMessage {

	private String dataRequestKey;
	private JsonObject rawResponse;
	private Function<JsonObject, Boolean> retryCallback;

	public LoginDeferredMessage(JsonObject rawResponse, String dataRequestKey,
			Function<JsonObject, Boolean> retryCallback) {
		this.dataRequestKey = dataRequestKey;
		this.rawResponse = rawResponse;
		this.retryCallback = retryCallback;
	}

	/**
	 * Retrieves the response json
	 * 
	 * @return JsonObject instance
	 */
	public JsonObject getRawResponse() {
		return rawResponse;
	}

	/**
	 * Retrieves the data request key
	 * 
	 * @return Data request key
	 */
	public String getDataRequestKey() {
		return dataRequestKey;
	}

	/**
	 * Retries the login attempt
	 * 
	 * @param request New request message (recommended to send the old message with
	 *                the missing data added to it)
	 */
	public void retry(JsonObject request) {
		retryCallback.apply(request);
	}

}
