package org.asf.edge.common.entities.messages.defaultmessages;

import java.util.HashMap;
import java.util.Map;

import org.asf.edge.common.util.TaggedMessageUtils;
import org.asf.edge.common.xmls.messages.MessageInfoData;

/**
 * 
 * Plugin message object
 * 
 * @author Sky Swimmer
 *
 */
public class WsPluginMessage extends WsGenericMessage {

	public String messageID = "edge:edge";
	public Map<String, String> messageData = new HashMap<String, String>();

	@Override
	public int messageTypeID() {
		return 36; // 36 is unused in vanilla
	}

	@Override
	public void serialize(MessageInfoData messageInfo) {
		// Serialize message
		rawObject.messageContentMembers = TaggedMessageUtils.writeTagged(
				Map.of("Line1", rawObject.messageContentMembers, "PluginMessage-" + encodeTagSafe(messageID),
						encodeTagSafe(TaggedMessageUtils.writeTagged(messageData))));
		rawObject.messageContentNonMembers = rawObject.messageContentMembers;

		// Write
		super.serialize(messageInfo);
	}

	private String encodeTagSafe(String data) {
		data = data.replace("<", "<lt>");
		data = data.replace(";", "<sl>");
		data = data.replace("[", "<bb>");
		data = data.replace("]", "<be>");
		data = data.replace("=", "<qe>");
		data = data.replace("&", "<am>");
		return data;
	}

	private String decodeTagSafe(String data) {
		data = data.replace("<lt>", "<");
		data = data.replace("<bb>", "[");
		data = data.replace("<be>", "]");
		data = data.replace("<qe>", "=");
		data = data.replace("<am>", "&");
		data = data.replace("<sl>", ";");
		return data;
	}

	@Override
	public void deserialize(MessageInfoData messageInfo) {
		// Read
		super.deserialize(messageInfo);

		// Deserialize message
		Map<String, String> d = TaggedMessageUtils.parseTagged(rawObject.messageContentMembers);
		for (String key : d.keySet()) {
			if (key.startsWith("PluginMessage-")) {
				messageID = decodeTagSafe(key.substring("PluginMessage-".length()));
				messageData = TaggedMessageUtils.parseTagged(decodeTagSafe(d.get(key)));
			}
		}
	}
}
