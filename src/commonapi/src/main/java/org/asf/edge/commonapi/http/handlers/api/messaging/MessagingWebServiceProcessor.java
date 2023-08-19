package org.asf.edge.commonapi.http.handlers.api.messaging;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.CommonUpdater;
import org.asf.edge.common.entities.messages.WsMessage;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
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

	@Function(allowedMethods = { "POST" })
	public FunctionResult getUserMessageQueue(FunctionInfo func) throws IOException {
		// Message queue request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);

		// Get messenger
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

	@Function(allowedMethods = { "POST" })
	public FunctionResult saveMessage(FunctionInfo func) throws IOException {
		// Message queue request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return response(400, "Bad request");
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			return response(404, "Not found");
		}

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);

		// Get messenger
		PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(account, save);

		// Parse request
		int id = Integer.parseInt(req.payload.get("userMessageQueueID"));
		boolean delete = req.payload.get("isDeleted").equalsIgnoreCase("true");

		// Handle
		if (delete)
			messenger.deleteMessage(id);
		else
			messenger.markMessageRead(id);

		// Return
		return ok("text/xml", req.generateXmlValue("bool", true));
	}

}
