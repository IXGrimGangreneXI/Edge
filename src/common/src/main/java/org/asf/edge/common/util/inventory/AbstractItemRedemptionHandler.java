package org.asf.edge.common.util.inventory;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;
import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData.ItemUpdateBlock;

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
		private ItemUpdateBlock[] updates;

		public RedemptionResult(boolean success, ItemUpdateBlock[] updates) {
			this.success = success;
			this.updates = updates;
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
		 * Retrieves the updated item blocks
		 * 
		 * @return Array of ItemUpdateBlock instances
		 */
		public ItemUpdateBlock[] getUpdates() {
			return updates;
		}

		public static RedemptionResult failure() {
			return new RedemptionResult(false, new ItemUpdateBlock[0]);
		}

		public static RedemptionResult success() {
			return new RedemptionResult(true, new ItemUpdateBlock[0]);
		}

		public static RedemptionResult failure(ItemUpdateBlock... updates) {
			return new RedemptionResult(false, updates);
		}

		public static RedemptionResult success(ItemUpdateBlock... updates) {
			return new RedemptionResult(true, updates);
		}

	}

}
