package org.asf.edge.common.entities.messages.defaultmessages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.asf.edge.common.util.TaggedMessageUtils;
import org.asf.edge.common.xmls.messages.MessageInfoData;

/**
 * 
 * Rank message object
 * 
 * @author Sky Swimmer
 *
 */
public class WsRankMessage extends WsGenericMessage {

	public int rankType = 0;
	public String[] levelUpMessage = new String[0];
	public String particle = "";

	@Override
	public int messageTypeID() {
		return 4;
	}

	@Override
	public String messageTypeName() {
		return "Rank";
	}

	@Override
	public void serialize(MessageInfoData messageInfo) {
		// Write
		super.serialize(messageInfo);

		// Serialize
		int i = 1;
		HashMap<String, String> taggedMessage = new HashMap<String, String>();
		for (String message : levelUpMessage)
			taggedMessage.put("Line" + i++, message);
		if (particle != null)
			taggedMessage.put("Particle", particle);
		if (rankType != 0)
			taggedMessage.put("Type", Integer.toString(rankType));
		if (taggedMessage.size() != 0)
			messageInfo.messageContentMembers = TaggedMessageUtils.writeTagged(taggedMessage);
	}

	@Override
	public void deserialize(MessageInfoData messageInfo) {
		// Read
		super.deserialize(messageInfo);

		// Parse
		ArrayList<String> msg = new ArrayList<String>();
		Map<String, String> taggedMessage = TaggedMessageUtils.parseTagged(messageInfo.messageContentMembers);
		if (taggedMessage.containsKey("Particle"))
			particle = taggedMessage.get("Particle");
		if (taggedMessage.containsKey("Type"))
			rankType = Integer.parseInt(taggedMessage.get("Type"));
		int i = 1;
		while (taggedMessage.containsKey("Line" + i)) {
			msg.add(taggedMessage.get("Line" + i));
			i++;
		}
		levelUpMessage = msg.toArray(t -> new String[t]);
	}
}
