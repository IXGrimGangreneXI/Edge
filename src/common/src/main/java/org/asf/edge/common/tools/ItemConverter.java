package org.asf.edge.common.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest.DefaultItemBlock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class ItemConverter {

	public static void main(String[] args) throws JsonMappingException, JsonProcessingException, IOException {
		// Usage:
		// <path-to-item-data-folder>

		// Prepare
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL, true);

		// Create object
		ItemRegistryManifest outp = new ItemRegistryManifest();

		// Add default items
		outp.defaultItems = new ItemRegistryManifest.DefaultItemsBlock();
		outp.defaultItems.defaultItems = new ItemRegistryManifest.DefaultItemBlock[] { defaultItem(1, 8977, 1, -1) };

		// Find defs
		ArrayList<ObjectNode> objs = new ArrayList<ObjectNode>();
		for (File file : new File(args[0]).listFiles(t -> t.getName().endsWith(".xml") && t.isFile())) {
			// Load def
			ObjectNode node = mapper.readValue(Files.readString(file.toPath()), ObjectNode.class);
			if (node.get("id").asInt() != 0) {
				objs.add(node);
				System.out.println("Item registered: " + node.get("id").asInt() + ": " + node.get("itn").asText());
			}
		}
		System.out.println(objs.size() + " items registered");

		// Add defs
		outp.itemDefs = objs.toArray(t -> new ObjectNode[t]);

		// Write
		Files.writeString(Path.of("itemdefs.xml"),
				mapper.writer().withDefaultPrettyPrinter().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
						.withRootName("ItemRegistryManifest").writeValueAsString(outp));
	}

	private static DefaultItemBlock defaultItem(int container, int id, int quantity, int uses) {
		DefaultItemBlock b = new DefaultItemBlock();
		b.itemID = id;
		b.uses = uses;
		b.quantity = quantity;
		b.inventoryID = container;
		return b;
	}

}
