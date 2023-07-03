package org.asf.edge.common.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class ItemCsvGenerator {

	public static void main(String[] args) throws IOException {
		// Usage:
		// <path-to-item-data-folder>

		// Prepare
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL, true);

		// Find defs
		ArrayList<ObjectNode> objs = new ArrayList<ObjectNode>();
		for (File file : new File(args[0]).listFiles(t -> t.getName().endsWith(".xml") && t.isFile())) {
			// Load def
			ObjectNode node = mapper.readValue(Files.readString(file.toPath()), ObjectNode.class);
			if (node.get("id").asInt() != 0) {
				objs.add(node);
			}
		}

		// Generate CSV
		String csv = "Item ID,Item Name,Item Description";
		for (ObjectNode def : objs) {
			csv += "\n\"" + def.get("id").asInt() + "\",\"" + def.get("itn").asText().replace("\"", "\"\"") + "\",\""
					+ def.get("d").asText().replace("\"", "\"\"") + "\"";
		}
		System.out.println(csv);
	}

}
