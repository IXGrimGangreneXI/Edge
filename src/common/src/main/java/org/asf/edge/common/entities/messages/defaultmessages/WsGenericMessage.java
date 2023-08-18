package org.asf.edge.common.entities.messages.defaultmessages;

import org.asf.edge.common.entities.messages.WsMessage;
import org.asf.edge.common.xmls.messages.MessageInfoData;

/**
 * 
 * Generic message object
 * 
 * @author Sky Swimmer
 *
 */
public class WsGenericMessage extends WsMessage {

	public MessageInfoData rawObject = new MessageInfoData();

	@Override
	public int messageTypeID() {
		return rawObject == null ? -1 : rawObject.typeID;
	}

	public int messageQueueID() {
		return rawObject.messageQueueID;
	}

	public String messageTypeName() {
		return rawObject == null ? null : rawObject.typeName;
	}

	@Override
	public void serialize(MessageInfoData messageInfo) {
		messageInfo.typeID = messageTypeID();
		messageInfo.typeName = messageTypeName();
		messageInfo.messageQueueID = rawObject.messageQueueID;
		messageInfo.audioUrlMembers = rawObject.audioUrlMembers;
		messageInfo.audioUrlNonMembers = rawObject.audioUrlNonMembers;
		messageInfo.fromUser = rawObject.fromUser;
		messageInfo.linkUrlMembers = rawObject.linkUrlMembers;
		messageInfo.linkUrlNonMembers = rawObject.linkUrlNonMembers;
		messageInfo.messageContentMembers = rawObject.messageContentMembers;
		messageInfo.messageContentNonMembers = rawObject.messageContentNonMembers;
		messageInfo.data = rawObject.data;
	}

	@Override
	public void deserialize(MessageInfoData messageInfo) {
		rawObject = messageInfo;
	}

	@Override
	public WsMessage createInstance() {
		return new WsGenericMessage();
	}

}
