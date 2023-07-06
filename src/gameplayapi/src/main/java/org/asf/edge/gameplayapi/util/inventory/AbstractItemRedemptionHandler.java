package org.asf.edge.gameplayapi.util.inventory;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.ItemUpdateBlock;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * Item Redemption Handler Abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AbstractItemRedemptionHandler {

	/**
	 * Checks if this handler can handle the given item
	 * 
	 * @param item Item to check
	 * @return True if compatible, false otherwise
	 */
	public abstract boolean canHandle(ItemInfo item);

	/**
	 * Defines if this handler delegates to the next, if false, execution finishes
	 * after this handler, if true, the previous handler will be run after this
	 * handler
	 * 
	 * @return True to enable delegation mode, false otherwise
	 */
	public boolean delegating() {
		return false;
	}

	/**
	 * Called to handle item redemption
	 * 
	 * @param item           Item definition to redeem
	 * @param req            Item redemption request
	 * @param account        Account object
	 * @param save           Account save object
	 * @param currencyUpdate Currency update object (edit values to add to currency)
	 * @return RedemptionResult instance
	 */
	public abstract RedemptionResult handleRedemption(ItemInfo item, ItemRedemptionInfo req, AccountObject account,
			AccountSaveContainer save, CurrencyUpdateBlock currencyUpdate);

	public static class RedemptionResult {
		private boolean success;
		private ItemUpdateBlock update;
		private ObjectNode infoBlock;

		public RedemptionResult(boolean success, ItemUpdateBlock update, ObjectNode infoBlock) {
			this.success = success;
			this.update = update;
			this.infoBlock = infoBlock;
		}

		/**
		 * Checks if the operation was successful
		 * 
		 * @return True if successful, false otherwise
		 */
		public boolean isSuccessful() {
			return success;
		}

		/**
		 * Retrieves the item update block
		 * 
		 * @return ItemUpdateBlock or null (may be null even if successful)
		 */
		public ItemUpdateBlock getUpdate() {
			return update;
		}

		/**
		 * Retrieves the item definition
		 * 
		 * @return ObjectNode instance or null (may be null even if successful)
		 */
		public ObjectNode getItemDef() {
			return infoBlock;
		}

		public static RedemptionResult failure() {
			return new RedemptionResult(false, null, null);
		}

		public static RedemptionResult success() {
			return new RedemptionResult(true, null, null);
		}

		public static RedemptionResult failure(ObjectNode info, ItemUpdateBlock update) {
			return new RedemptionResult(false, update, info);
		}

		public static RedemptionResult success(ObjectNode info, ItemUpdateBlock update) {
			return new RedemptionResult(true, update, info);
		}

	}

}
