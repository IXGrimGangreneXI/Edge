package org.asf.edge.common.util;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * Avatar Downgrade Utility
 * 
 * 
 */
public class AvatarDowngrader {

	/**
	 * Avatar downgrade utility
	 * 
	 * @param avi     Avatar object
	 * @param account Account instance
	 * @param save    Account save
	 * @param vX      Version X
	 * @param vY      Version Y
	 * @param vZ      Version Z
	 * @return Avatar object
	 */
	public static ObjectNode downgradeAvatar(ObjectNode avi, AccountObject account, AccountSaveContainer save, int vX,
			int vY, int vZ) {
		// Find version part
		if (avi.has("Part")) {
			// Load parts
			JsonNode partRoot = avi.get("Part");
			ObjectNode[] parts;
			if (partRoot.isArray()) {
				parts = new ObjectNode[partRoot.size()];
				for (int i = 0; i < parts.length; i++)
					parts[i] = (ObjectNode) partRoot.get(i);
			} else
				parts = new ObjectNode[] { (ObjectNode) partRoot };

			// Go through parts
			int[] verData = null;
			for (ObjectNode part : parts) {
				if (part.has("PartType") && part.get("PartType").asText().equals("Version") && part.has("Offsets")
						&& part.get("Offsets").has("Offset")) {
					// Found version part
					JsonNode offsetsRoot = part.get("Offsets").get("Offset");
					ObjectNode[] offsets;
					if (offsetsRoot.isArray()) {
						offsets = new ObjectNode[offsetsRoot.size()];
						for (int i = 0; i < offsets.length; i++)
							offsets[i] = (ObjectNode) offsetsRoot.get(i);
					} else
						offsets = new ObjectNode[] { (ObjectNode) offsetsRoot };
					if (offsets.length == 1) {
						verData = new int[3];
						verData[0] = offsets[0].get("X").asInt();
						verData[1] = offsets[0].get("Y").asInt();
						verData[2] = offsets[0].get("Z").asInt();
					}
					break;
				}
			}
			if (verData == null) {
				// Create new version

				// Copy avi
				avi = avi.deepCopy();
				partRoot = avi.get("Part");

				// Update
				// TODO: mapping API

				// Create version
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode ver = mapper.createObjectNode();
				ObjectNode off = mapper.createObjectNode();
				ObjectNode offR = mapper.createObjectNode();
				offR.set("Offset", off);
				off.put("X", vX);
				off.put("Y", vY);
				off.put("Z", vZ);
				ver.put("PartType", "Version");
				ver.set("Offsets", offR);
				if (partRoot.isArray()) {
					// Update array
					((ArrayNode) partRoot).add(ver);
				} else {
					// Create array
					ArrayNode arr = mapper.createArrayNode();
					arr.add(partRoot);
					arr.add(ver);
					avi.remove("Part");
					avi.set("Part", arr);
				}
			} else if (verData[0] != vX || verData[1] != vY || verData[2] != vZ) {
				// Update version

				// Copy avi
				avi = avi.deepCopy();
				partRoot = avi.get("Part");
				if (partRoot.isArray()) {
					parts = new ObjectNode[partRoot.size()];
					for (int i = 0; i < parts.length; i++)
						parts[i] = (ObjectNode) partRoot.get(i);
				} else
					parts = new ObjectNode[] { (ObjectNode) partRoot };

				// Update
				// TODO: mapping API

				// Set version
				for (ObjectNode part : parts) {
					if (part.has("PartType") && part.get("PartType").asText().equals("Version") && part.has("Offsets")
							&& part.get("Offsets").has("Offset")) {
						// Found version part
						JsonNode offsetsRoot = part.get("Offsets").get("Offset");
						ObjectNode[] offsets;
						if (offsetsRoot.isArray()) {
							offsets = new ObjectNode[offsetsRoot.size()];
							for (int i = 0; i < offsets.length; i++)
								offsets[i] = (ObjectNode) offsetsRoot.get(i);
						} else
							offsets = new ObjectNode[] { (ObjectNode) offsetsRoot };
						offsets[0].put("X", vX);
						offsets[0].put("Y", vY);
						offsets[0].put("Z", vZ);
						break;
					}
				}
			}
		}

		// Return
		return avi;
	}

}
