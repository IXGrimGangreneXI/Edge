package org.asf.edge.modules.gridclient.phoenix.exceptions;

import org.asf.edge.modules.gridclient.phoenix.DisconnectReason;

public class PhoenixConnectException extends Exception {

	private static final long serialVersionUID = 1L;

	private DisconnectReason disconnectReason;

	public PhoenixConnectException(DisconnectReason disconnectReason) {
		super("Phoenix error: " + disconnectReason.getDisconnectReason());
		this.disconnectReason = disconnectReason;
	}

	public PhoenixConnectException(String message, DisconnectReason disconnectReason) {
		super(message);
		this.disconnectReason = disconnectReason;
	}

	public DisconnectReason getDisconnectReason() {
		return disconnectReason;
	}

}
