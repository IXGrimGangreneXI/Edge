package org.asf.edge.common.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.nexus.common.services.ServiceImplementationPriorityLevels;
import org.asf.nexus.common.services.ServiceManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class ItemCsvGenerator {

	public static void main(String[] args) throws IOException {
		// Prepare
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);

		// Prepare
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL, true);

		// Generate CSVs
		System.out.println("Generating detailed item list...");
		String csv = "Item ID,"
				//
				+ "Item Name,"
				//
				+ "Item Description,"
				//
				+ "Asset Name,"
				//
				+ "Icon Name,"
				//
				+ "Normal cost (non-sale),"
				//
				+ "Selling Factor,"
				//
				+ "Uses,"
				//
				+ "Attributes,"
				//
				+ "Categories,"
				//
				+ "Item relations,"
				//
				+ "Availability,"
				//
				+ "Rank ID,"
				//
				+ "Creative Points,"
				//
				+ "Inventory Max,"
				//
				+ "Lock State,"
				//
				+ "Stackable (unused in client),"
				//
				+ "Allow Stacking (unused in client),"
				//
				+ "Geometry,"
				//
				+ "Textures,"
				//
				+ "Reward type ID,"
				//
				+ "Points\n";
		int i = 0;
		FileOutputStream fO = new FileOutputStream("detaileditemlist.csv");
		fO.write(csv.getBytes("UTF-8"));
		csv = "";
		String lastProg = "";
		for (ObjectNode def : Stream.of(ItemManager.getInstance().getAllItemDefinitions()).map(t -> t.getRawObject())
				.toArray(t -> new ObjectNode[t])) {
			// Write ID
			csv = "\n\"" + def.get("id").asInt() + "\",";

			// Write name
			if (def.has("itn") && !def.get("itn").asText().isEmpty())
				csv += "\"\"\"" + def.get("itn").asText().replace("\"", "\"\"") + "\"\"\",";
			else
				csv += ",";

			// Write description
			if (def.has("d") && !def.get("d").asText().isEmpty())
				csv += "\"" + def.get("d").asText().replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write asset name
			if (def.has("an"))
				csv += "\"" + def.get("an").asText().replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write icon name
			if (def.has("icn"))
				csv += "\"" + def.get("icn").asText().replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write cost
			int costGems = 0;
			int costCoins = 0;
			if (def.has("ct"))
				costCoins = def.get("ct").asInt();
			if (def.has("ct2"))
				costGems = def.get("ct2").asInt();
			if (costGems > 0)
				csv += "\"" + costGems + " gems\",";
			else if (costCoins > 0)
				csv += "\"" + costCoins + " coins\",";
			else
				csv += "free or not in store,";

			// Write selling factor
			if (def.has("sf"))
				csv += def.get("sf").asInt() + ",";
			else
				csv += ",";

			// Uses
			if (def.has("u"))
				csv += def.get("u").asInt() + ",";
			else
				csv += ",";

			// Write attributes
			Attribute[] attrs = getAttributes(def);
			String str = "";
			for (Attribute attr : attrs) {
				if (str.isEmpty())
					str = attr.key + " = " + attr.value;
				else
					str += "\n" + attr.key + " = " + attr.value;
			}
			if (!str.isEmpty())
				csv += "\"" + str.replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write categories
			Category[] cats = getCategories(def);
			str = "";
			for (Category cat : cats) {
				if (str.isEmpty())
					str = cat.categoryName + " (ID: " + cat.id + ", icon: " + cat.iconName + ")";
				else
					str += "\n" + cat.categoryName + " (ID: " + cat.id + ", icon: " + cat.iconName + ")";
			}
			if (!str.isEmpty())
				csv += "\"" + str.replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write relations
			Relation[] relations = getRelations(def);
			str = "";
			for (Relation rel : relations) {
				if (str.isEmpty())
					str = rel.type + ": " + (rel.quantity == 0 ? 1 : rel.quantity) + "x ID " + rel.itemID + " (weight: "
							+ rel.weight + ")";
				else
					str += "\n" + rel.type + ": " + (rel.quantity == 0 ? 1 : rel.quantity) + "x ID " + rel.itemID
							+ " (weight: " + rel.weight + ")";
			}
			if (!str.isEmpty())
				csv += "\"" + str.replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write availability
			Availability[] avs = getAvailability(def);
			str = "";
			for (Availability av : avs) {
				if (str.isEmpty())
					str = av.startDate + " - " + av.endDate;
				else
					str += "\n" + av.startDate + " - " + av.endDate;
			}
			if (!str.isEmpty())
				csv += "\"" + str.replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write rank ID
			if (def.has("rid"))
				csv += def.get("rid").asInt() + ",";
			else
				csv += ",";

			// Write creative points
			if (def.has("cp"))
				csv += def.get("cp").asInt() + ",";
			else
				csv += "\"\",";

			// Write max
			if (def.has("im"))
				csv += def.get("im").asInt() + ",";
			else
				csv += ",";

			// Write lock state
			if (def.has("l"))
				csv += (def.get("l").asBoolean() ? "Locked" : "Not locked") + ",";
			else
				csv += ",";

			// Write stackable state
			if (def.has("s"))
				csv += (def.get("s").asBoolean() ? "Stackable" : "Not stackable") + ",";
			else
				csv += ",";

			// Write stackable state
			if (def.has("as"))
				csv += (def.get("as").asBoolean() ? "Allows stacking" : "Denies stacking") + ",";
			else
				csv += ",";

			// Write geometry
			if (def.has("g") && !def.get("g").asText().isEmpty())
				csv += "\"" + def.get("g").asText().replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write textures
			Texture[] ts = getTextures(def);
			str = "";
			for (Texture t : ts) {
				if (str.isEmpty())
					str = t.typeName + " (asset: " + t.assetName + ", x offset: " + t.offsetX + ", y offset: "
							+ t.offsetY + ")";
				else
					str += "\n" + t.typeName + " (asset: " + t.assetName + ", x offset: " + t.offsetX + ", y offset: "
							+ t.offsetY + ")";
			}
			if (!str.isEmpty())
				csv += "\"" + str.replace("\"", "\"\"") + "\",";
			else
				csv += ",";

			// Write reward type ID
			if (def.has("rtid"))
				csv += def.get("rtid").asInt() + ",";
			else
				csv += ",";

			// Write pints
			if (def.has("p"))
				csv += def.get("p").asInt();

			// Write
			fO.write(csv.getBytes("UTF-8"));
			csv = "";

			// Log
			i++;
			String progress = (int) ((100f / (float) ItemManager.getInstance().getAllItemDefinitions().length)
					* (float) i) + "%";
			if (!lastProg.equals(progress)) {
				lastProg = progress;
				System.out.println("Progress: " + progress);
			}
		}
		fO.close();
		System.out.println("Generating simplified item list...");
		csv = "Item ID,Item Name,Item Description\n";
		for (ObjectNode def : Stream.of(ItemManager.getInstance().getAllItemDefinitions()).map(t -> t.getRawObject())
				.toArray(t -> new ObjectNode[t])) {
			// Write ID
			csv += "\n\"" + def.get("id").asInt() + "\",";

			// Write name
			if (def.has("itn") && !def.get("itn").asText().isEmpty())
				csv += "\"\"\"" + def.get("itn").asText().replace("\"", "\"\"") + "\"\"\",";
			else
				csv += ",";

			// Write description
			if (def.has("d") && !def.get("d").asText().isEmpty())
				csv += "\"" + def.get("d").asText().replace("\"", "\"\"") + "\",";
			else
				csv += ",";
		}
		Files.writeString(Path.of("itemlist.csv"), csv);
	}

	public static class Availability {
		public String startDate;
		public String endDate;
	}

	public static class Attribute {
		public String key;
		public JsonNode value;
	}

	public static class Category {
		public int id;
		public String categoryName;
		public String iconName;
	}

	public static class Relation {
		public String type;
		public int itemID;
		public int weight;
		public int quantity;
	}

	public static class Texture {
		public String assetName;
		public String typeName;
		public String offsetX;
		public String offsetY;
	}

	private static Texture[] getTextures(ObjectNode raw) {
		if (!raw.has("t"))
			return new Texture[0];

		ArrayList<Texture> nodes = new ArrayList<Texture>();

		// Check item def
		JsonNode node = raw.get("t");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("n")) {
					Texture t = new Texture();
					t.assetName = n.get("n").asText();
					t.typeName = n.get("t").asText();
					t.offsetX = n.get("x").asText();
					t.offsetY = n.get("y").asText();
					nodes.add(t);
				}
			}
		} else if (node.has("n")) {
			// Go through single item
			Texture t = new Texture();
			t.assetName = node.get("n").asText();
			t.typeName = node.get("t").asText();
			t.offsetX = node.get("x").asText();
			t.offsetY = node.get("y").asText();
			nodes.add(t);
		}

		// Return
		return nodes.toArray(t -> new Texture[t]);
	}

	private static Availability[] getAvailability(ObjectNode raw) {
		if (!raw.has("av"))
			return new Availability[0];

		ArrayList<Availability> nodes = new ArrayList<Availability>();

		// Check item def
		JsonNode node = raw.get("av");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("sdate")) {
					Availability a = new Availability();
					a.startDate = n.get("sdate").asText();
					a.endDate = n.get("edate").asText();
					nodes.add(a);
				}
			}
		} else if (node.has("sdate")) {
			// Go through single item
			Availability a = new Availability();
			a.startDate = node.get("sdate").asText();
			a.endDate = node.get("edate").asText();
			nodes.add(a);
		}

		// Return
		return nodes.toArray(t -> new Availability[t]);
	}

	private static Relation[] getRelations(ObjectNode raw) {
		if (!raw.has("r"))
			return new Relation[0];

		ArrayList<Relation> nodes = new ArrayList<Relation>();

		// Check item def
		JsonNode node = raw.get("r");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("t")) {
					Relation r = new Relation();
					r.type = n.get("t").asText();
					r.itemID = n.get("id").asInt();
					r.weight = n.get("wt").asInt();
					r.quantity = n.get("q").asInt();
					nodes.add(r);
				}
			}
		} else if (node.has("t")) {
			// Go through single item
			Relation r = new Relation();
			r.type = node.get("t").asText();
			r.itemID = node.get("id").asInt();
			r.weight = node.get("wt").asInt();
			r.quantity = node.get("q").asInt();
			nodes.add(r);
		}

		// Return
		return nodes.toArray(t -> new Relation[t]);
	}

	private static Category[] getCategories(ObjectNode raw) {
		if (!raw.has("c"))
			return new Category[0];

		// Go through categories
		ArrayList<Category> cats = new ArrayList<Category>();
		JsonNode node = raw.get("c");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("cid") && n.has("cn") && n.has("i")) {
					Category c = new Category();
					c.id = n.get("cid").asInt();
					c.categoryName = n.get("cn").asText();
					c.iconName = n.get("i").asText();
					cats.add(c);
				}
			}
		} else if (node.has("cid") && node.has("cn") && node.has("i")) {
			// Go through single item
			Category c = new Category();
			c.id = node.get("cid").asInt();
			c.categoryName = node.get("cn").asText();
			c.iconName = node.get("i").asText();
			cats.add(c);
		}
		return cats.toArray(t -> new Category[t]);
	}

	private static Attribute[] getAttributes(ObjectNode raw) {
		if (!raw.has("at"))
			return new Attribute[0];

		// Go through attributes
		ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		JsonNode node = raw.get("at");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("k") && n.has("v")) {
					Attribute attr = new Attribute();
					attr.key = n.get("k").asText();
					attr.value = n.get("v");
					attrs.add(attr);
				}
			}
		} else if (node.has("k") && node.has("v")) {
			// Go through single item
			Attribute attr = new Attribute();
			attr.key = node.get("k").asText();
			attr.value = node.get("v");
			attrs.add(attr);
		}
		return attrs.toArray(t -> new Attribute[t]);
	}

}
