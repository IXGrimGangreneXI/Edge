package org.asf.edge.gameplayapi.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
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
		// "<path to quest dump directory>"

		// Prepare
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL, true);

		// Prepare output
		QuestRegistryManifest outp = new QuestRegistryManifest();

		// Scan into registry
		outp.defaultQuestDefs = new QuestRegistryManifest.QuestDefsBlock();
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

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	private static class QuestRegistryManifest {

		public QuestDefsBlock defaultQuestDefs;

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class QuestDefsBlock {

			@JsonProperty("QuestDef")
			@JacksonXmlElementWrapper(useWrapping = false)
			public ObjectNode[] questDefs;

		}
	}

}
