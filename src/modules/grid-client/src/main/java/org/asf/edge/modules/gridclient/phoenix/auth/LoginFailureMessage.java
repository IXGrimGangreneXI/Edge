package org.asf.edge.modules.gridclient.phoenix.auth;

import com.google.gson.JsonObject;

/**
 * 
 * Login failure message container
 * 
 * @author Sky Swimmer
 *
 */
public class LoginFailureMessage {

	private String error;
	private String errorMessage;

	private JsonObject rawResponse;

	public LoginFailureMessage(JsonObject rawResponse, String error, String errorMessage) {
		this.error = error;
		this.errorMessage = errorMessage;
		this.rawResponse = rawResponse;
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
	 * Retrieves the error type
	 * 
	 * @return Error type string
	 */
	public String getError() {
		return error;
	}

	/**
	 * Retrieves the message type
	 * 
	 * @return Error message string
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

}
