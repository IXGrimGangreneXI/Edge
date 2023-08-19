package org.asf.edge.common.http.apihandlerutils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 
 * Throw to return a HTTP error code
 * 
 * @author Sky Swimmer
 *
 */
public class HttpException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int statusCode;
	private String statusMessage;
	private String bodyMediaType;
	private byte[] body;

	public HttpException(int statusCode, String statusMessage) {
		super(statusCode + " " + statusMessage);
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
	}

	public HttpException(int statusCode, String statusMessage, byte[] body) {
		this(statusCode, statusMessage);
		this.body = body;
	}

	public HttpException(int statusCode, String statusMessage, String body) {
		this(statusCode, statusMessage, body.getBytes(StandardCharsets.UTF_8));
	}

	public HttpException(int statusCode, String statusMessage, String bodyMediaType, byte[] body) {
		this(statusCode, statusMessage);
		this.body = body;
		this.bodyMediaType = bodyMediaType;
	}

	public HttpException(int statusCode, String statusMessage, String bodyMediaType, String body) {
		this(statusCode, statusMessage, bodyMediaType, body.getBytes(StandardCharsets.UTF_8));
	}

	public byte[] getBody() {
		return body;
	}

	public String getBodyMediaType() {
		return bodyMediaType;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public int getStatusCode() {
		return statusCode;
	}

}
