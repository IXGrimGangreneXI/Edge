package org.asf.edge.common.http.functions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * 
 * Function result object, holds result values for function calls
 * 
 * @author Sky Swimmer
 *
 */
public class FunctionResult {

	private String statusMessage;
	private int statusCode;

	private boolean hasResponseBody;
	private String mediaType;
	private InputStream responseBody;
	private long contentLength = -1;

	/**
	 * Creates a default function result object with no response body (errors will
	 * use default error page)
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 */
	public FunctionResult(int statusCode, String statusMessage) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, String mediaType, InputStream responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.mediaType = mediaType;
		this.responseBody = responseBody;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, InputStream responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.responseBody = responseBody;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param contentLength Response body length
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, String mediaType, long contentLength,
			InputStream responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.mediaType = mediaType;
		this.responseBody = responseBody;
		this.contentLength = contentLength;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param contentLength Response body length
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, long contentLength, InputStream responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.responseBody = responseBody;
		this.contentLength = contentLength;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, String mediaType, byte[] responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.mediaType = mediaType;
		this.responseBody = new ByteArrayInputStream(responseBody);
		this.contentLength = responseBody.length;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, byte[] responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.responseBody = new ByteArrayInputStream(responseBody);
		this.contentLength = responseBody.length;
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, String mediaType, String responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		this.mediaType = mediaType;
		try {
			this.responseBody = new ByteArrayInputStream(responseBody.getBytes("UTF-8"));
			this.contentLength = responseBody.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a default function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param responseBody  Response body
	 */
	public FunctionResult(int statusCode, String statusMessage, String responseBody) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.hasResponseBody = true;
		try {
			this.responseBody = new ByteArrayInputStream(responseBody.getBytes("UTF-8"));
			this.contentLength = responseBody.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public long getContentLength() {
		return contentLength;
	}

	public boolean hasResponseBody() {
		return hasResponseBody;
	}

	public String getResponseMediaType() {
		return mediaType;
	}

	public InputStream getResponseBodyStream() {
		return responseBody;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public int getStatusCode() {
		return statusCode;
	}

}
