package org.asf.edge.common.entities.items;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.attributes.ItemAttributeData;
import org.asf.edge.common.xmls.items.categories.ItemCategoryData;

/**
 * 
 * Item information container
 * 
 * @author Sky Swimmer
 *
 */
public class ItemInfo {
	private int id;
	private String name;
	private String description;
	private ItemDefData raw;
	private ItemCategoryInfo[] categories;
	private ItemAttributeInfo[] attributes;

	private int costGems;
	private int costCoins;

	public ItemInfo(int id, String name, String description, ItemDefData raw) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.raw = raw;

		costCoins = raw.costCoins;
		costGems = raw.costGems;

		loadCategories();
		loadAttributes();
	}

	private void loadAttributes() {
		// Go through attributes
		ArrayList<ItemAttributeInfo> attrs = new ArrayList<ItemAttributeInfo>();
		for (ItemAttributeData attr : raw.attributes) {
			attrs.add(new ItemAttributeInfo(attr.key, attr.value));
		}
		attributes = attrs.toArray(t -> new ItemAttributeInfo[t]);
	}

	private void loadCategories() {
		// Go through categories
		ArrayList<ItemCategoryInfo> cats = new ArrayList<ItemCategoryInfo>();
		for (ItemCategoryData cat : raw.categories) {
			cats.add(new ItemCategoryInfo(cat.categoryID, cat.categoryName, cat.iconName));
		}
		categories = cats.toArray(t -> new ItemCategoryInfo[t]);
	}

	public static class CostInfo {
		public boolean isGems;
		public boolean isCoins;
		public boolean isFree;

		public int cost;
	}

	/**
	 * Retrieves the item final cost
	 * 
	 * @param member True to include member-only sales in cost calculation, false
	 *               otherwise
	 * @return CostInfo instance
	 */
	public CostInfo getFinalCost(boolean member) {
		CostInfo cost = getCost();

		// Apply member sales
		ItemManager manager = ItemManager.getInstance();
		float costFinal = cost.cost;
		if (member) {
			for (ItemSaleInfo sale : manager.getActiveSales()) {
				if (sale.isMemberOnly()) {
					// Check sale
					boolean valid = false;
					if (IntStream.of(sale.getItemIDs()).anyMatch(t -> t == id))
						valid = true;
					else if (IntStream.of(sale.getCategories())
							.anyMatch(t -> Stream.of(getCategories()).anyMatch(t2 -> t2.getCategoryID() == t)))
						valid = true;
					if (!valid)
						continue;

					// Apply modifier
					float modifier = 0;
					if (sale.getSaleModifier() >= 1) {
						modifier = 0f;
					} else if (sale.getSaleModifier() >= 0) {
						modifier = 1f - sale.getSaleModifier();
					} else
						modifier = 1f;
					costFinal = costFinal * modifier;
				}
			}
		}

		// Apply default sales
		for (ItemSaleInfo sale : manager.getActiveSales()) {
			if (!sale.isMemberOnly()) {
				// Check sale
				boolean valid = false;
				if (IntStream.of(sale.getItemIDs()).anyMatch(t -> t == id))
					valid = true;
				else if (IntStream.of(sale.getCategories())
						.anyMatch(t -> Stream.of(getCategories()).anyMatch(t2 -> t2.getCategoryID() == t)))
					valid = true;
				if (!valid)
					continue;

				float modifier = 0;
				if (sale.getSaleModifier() >= 1) {
					modifier = 0f;
				} else if (sale.getSaleModifier() >= 0) {
					modifier = 1f - sale.getSaleModifier();
				} else
					modifier = 1f;
				costFinal = costFinal * modifier;
			}
		}

		// Round
		costFinal = new BigDecimal(costFinal).setScale(0, RoundingMode.HALF_UP).floatValue();
		cost.cost = (int) costFinal;

		// Return result
		return cost;
	}

	/**
	 * Retrieves the item cost
	 * 
	 * @return Item cost as integer
	 */
	public CostInfo getCost() {
		if (costsGems()) {
			CostInfo cost = new CostInfo();
			cost.isGems = true;
			cost.cost = costGems;
			return cost;
		} else {
			CostInfo cost = new CostInfo();
			if (costCoins <= 0)
				cost.isFree = true;
			else
				cost.isCoins = true;
			cost.cost = costCoins;
			return cost;
		}
	}

	/**
	 * Retrieves the price in coins (may be an invalid amount if free or if its
	 * bought with gems)
	 * 
	 * @return Item price in coins
	 */
	public int getCoinCost() {
		return costCoins;
	}

	/**
	 * Retrieves the price in gems (may be an invalid amount if free or if its
	 * bought with coins)
	 * 
	 * @return Item price in gems
	 */
	public int getGemCost() {
		return costGems;
	}

	/**
	 * Checks if the item is free
	 * 
	 * @return True if the item is free, false otherwise
	 */
	public boolean isFree() {
		return costGems <= 0 && costCoins <= 0;
	}

	/**
	 * Checks if the item costs gems
	 * 
	 * @return True if the item costs gems, false otherwise
	 */
	public boolean costsGems() {
		return costGems > 0;
	}

	/**
	 * Checks if the item costs coins
	 * 
	 * @return True if the item costs coins, false otherwise
	 */
	public boolean costsCoins() {
		return costGems <= 0;
	}

	/**
	 * Retrieves the shop categories the item is in
	 * 
	 * @return Array of ItemCategoryInfo instances
	 */
	public ItemCategoryInfo[] getCategories() {
		return categories;
	}

	/**
	 * Retrieves the item attributes
	 * 
	 * @return Array of ItemAttribute instances
	 */
	public ItemAttributeInfo[] getAttributes() {
		return attributes;
	}

	/**
	 * Checks if an attribute is present
	 * 
	 * @param key Attribute key
	 * @return True if present, false otherwise
	 */
	public boolean hasAttribute(String key) {
		return Stream.of(attributes).anyMatch(t -> t.getKey().equals(key));
	}

	/**
	 * Retrieves attributes by key
	 * 
	 * @param key Attribute key
	 * @return ItemAttributeInfo instance or null
	 */
	public ItemAttributeInfo getAttribute(String key) {
		Optional<ItemAttributeInfo> attr = Stream.of(attributes).filter(t -> t.getKey().equals(key)).findFirst();
		if (attr.isEmpty())
			return null;
		return attr.get();
	}

	/**
	 * Retrieves the item ID
	 * 
	 * @return Item ID
	 */
	public int getID() {
		return id;
	}

	/**
	 * Retrieves the item name
	 * 
	 * @return Item name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the item description
	 * 
	 * @return Item description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the raw item object
	 * 
	 * @return ItemDefData instance
	 */
	public ItemDefData getRawObject() {
		return raw;
	}

	public void reloadDef() {
		// Reload
		id = raw.id;
		name = raw.name;
		description = raw.description;

		costCoins = raw.costCoins;
		costGems = raw.costGems;

		loadCategories();
		loadAttributes();
	}
}
