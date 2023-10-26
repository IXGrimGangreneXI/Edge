package org.asf.edge.commonapi.http.handlers.api.messaging;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.CommonUpdater;
import org.asf.edge.common.SentinelUpdateManager;
import org.asf.edge.common.entities.messages.WsMessage;
import org.asf.edge.common.http.EdgeWebService;
import org.asf.edge.common.http.functions.*;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.xmls.messages.MessageInfoData;
import org.asf.edge.common.xmls.messages.MessageInfoList;
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

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult getUserMessageQueue(ServiceRequestInfo req, AccountObject account, AccountSaveContainer save)
			throws IOException {
		// Get messenger
		SentinelUpdateManager.warnPlayerIfUpdating(account);
		CommonUpdater.warnPlayerIfUpdating(account);
		PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(account, save);

		// List messages
		WsMessage[] messages = messenger
				.getQueuedMessages(!req.payload.get("showOldMessages").equalsIgnoreCase("true"));

		// Prepare response
		MessageInfoList list = new MessageInfoList();
		list.messages = new MessageInfoData[messages.length];

		// Populate response
		for (int i = 0; i < messages.length; i++) {
			WsMessage msg = messages[i];
			MessageInfoData info = new MessageInfoData();
			info.typeID = msg.messageTypeID();

			// Serialize
			msg.serialize(info);

			// Add
			list.messages[i] = info;
		}

		// Return
		String str = req.generateXmlValue("ArrayOfMessageInfo", list);
		return ok("text/xml", str);
	}

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult saveMessage(ServiceRequestInfo req, AccountObject account, AccountSaveContainer save,
			@SodRequestParam int userMessageQueueID, @SodRequestParam boolean isDeleted) throws IOException {
		// Get messenger
		PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(account, save);

		// Handle
		if (isDeleted)
			messenger.deleteMessage(userMessageQueueID);
		else
			messenger.markMessageRead(userMessageQueueID);

		// Return
		return ok("text/xml", req.generateXmlValue("bool", true));
	}

}
