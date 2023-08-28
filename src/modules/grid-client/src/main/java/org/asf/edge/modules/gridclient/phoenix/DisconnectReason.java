package org.asf.edge.modules.gridclient.phoenix;

public class DisconnectReason {

	private String disconnectReason;
	private String[] disconnectReasonArgs;

	public DisconnectReason(String disconnectReason, String... disconnectReasonArgs) {
		this.disconnectReason = disconnectReason;
		this.disconnectReasonArgs = disconnectReasonArgs;
	}

	public String getReason() {
		return disconnectReason;
	}

	public String[] getReasonArgs() {
		return disconnectReasonArgs;
	}

}
