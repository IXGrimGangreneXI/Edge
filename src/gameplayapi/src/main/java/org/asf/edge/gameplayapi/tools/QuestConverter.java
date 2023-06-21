package org.asf.edge.gameplayapi.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;

import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest;
import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest.DefaultStartedQuestsBlock;
import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest.DefaultUnlockedQuestsBlock;
import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest.QuestDefsBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class QuestConverter {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	private static class QuestListData {

		@JsonProperty("Mission")
		@JacksonXmlElementWrapper(useWrapping = false)
		public ObjectNode[] missions;

	}

	public static void main(String[] args) throws JsonSyntaxException, IOException {
		// Expected arguments:
		// "<path to quest dump directory>" "<path to sniffer log made with a new
		// account or new guest account being the first game join>"

		// Parse sniff
		System.out.println("Parsing sniff...");
		String activeMissionsData = null;
		String upcomingMissionsData = null;
		for (String line : Files.readAllLines(Path.of(args[1]))) {
			if (line.isBlank())
				continue;

			// Parse json
			JsonObject packet = JsonParser.parseString(line).getAsJsonObject();
			if (!packet.has("type") || packet.get("type").getAsString().equals("http")) {
				// Check request
				URL u = new URL(packet.get("request").getAsJsonObject().get("url").getAsString());
				if (u.getPath().equalsIgnoreCase("/V2/ContentWebService.asmx/GetUserActiveMissionState")) {
					if (activeMissionsData != null)
						continue;
					System.out.println("Found active mission data.");
					activeMissionsData = new String(
							Base64.getDecoder()
									.decode(packet.get("response").getAsJsonObject().get("responseBody").getAsString()),
							"UTF-8");
				} else if (u.getPath().equalsIgnoreCase("/V2/ContentWebService.asmx/GetUserUpcomingMissionState")) {
					if (upcomingMissionsData != null)
						continue;
					System.out.println("Found upcoming mission data.");
					upcomingMissionsData = new String(
							Base64.getDecoder()
									.decode(packet.get("response").getAsJsonObject().get("responseBody").getAsString()),
							"UTF-8");
				}
			}
		}

		// Check
		if (activeMissionsData == null || upcomingMissionsData == null) {
			System.err.println(
					"Missing GetUserActiveMissionState and/or GetUserUpcomingMissionState calls in sniffer data.");
			System.exit(1);
			return;
		}

		// Prepare
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL, true);

		// Prepare output
		QuestRegistryManifest outp = new QuestRegistryManifest();

		// Load active missions
		int i = 0;
		System.out.println("Scanning active missions...");
		QuestListData activeD = mapper.readValue(activeMissionsData, QuestListData.class);
		ObjectNode[] active = activeD.missions;
		outp.defaultStartedQuests = new DefaultStartedQuestsBlock();
		outp.defaultStartedQuests.defaultStartedQuests = new int[active.length];
		for (ObjectNode node : active) {
			System.out.println("Added active quest to registry: " + node.get("I").asInt());
			outp.defaultStartedQuests.defaultStartedQuests[i++] = node.get("I").asInt();
		}

		// Load default unlocked missions
		i = 0;
		System.out.println("Scanning active missions...");
		QuestListData unlockedD = mapper.readValue(upcomingMissionsData, QuestListData.class);
		ObjectNode[] unlocked = unlockedD.missions;
		outp.defaultUnlockedQuests = new DefaultUnlockedQuestsBlock();
		outp.defaultUnlockedQuests.defaultUnlockedQuests = new int[active.length];
		for (ObjectNode node : unlocked) {
			System.out.println("Added default unlocked quest to registry: " + node.get("I").asInt());
			outp.defaultUnlockedQuests.defaultUnlockedQuests[i++] = node.get("I").asInt();
		}

		// Scan into registry
		outp.defaultQuestDefs = new QuestDefsBlock();
		ArrayList<ObjectNode> nodes = new ArrayList<ObjectNode>();
		for (File file : new File(args[0]).listFiles(t -> t.getName().endsWith(".xml") && t.isFile())) {
			// Load def
			ObjectNode node = mapper.readValue(Files.readString(file.toPath()), ObjectNode.class);
			if (node.get("I").asInt() != 0) {
				nodes.add(node);
				System.out.println("Quest registered: " + node.get("I").asInt());
			}
		}
		System.out.println(nodes.size() + " quests registered");
		outp.defaultQuestDefs.questDefs = nodes.toArray(t -> new ObjectNode[t]);

		// Done
		Files.writeString(Path.of("questdata.xml"),
				mapper.writer().withDefaultPrettyPrinter().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
						.withRootName("QuestRegistryManifest").writeValueAsString(outp));
	}

}
