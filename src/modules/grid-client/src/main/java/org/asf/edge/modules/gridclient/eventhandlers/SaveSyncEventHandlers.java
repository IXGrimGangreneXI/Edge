package org.asf.edge.modules.gridclient.eventhandlers;

import org.asf.edge.common.events.accounts.saves.AccountSaveAuthenticatedEvent;
import org.asf.edge.common.events.accounts.saves.AccountSaveCreatedEvent;
import org.asf.edge.common.events.accounts.saves.AccountSaveDeletedEvent;
import org.asf.edge.common.events.accounts.saves.AccountSaveUsernameUpdateEvent;
import org.asf.edge.modules.gridclient.utils.GridSaveUtil;
import org.asf.nexus.events.EventListener;
import org.asf.nexus.events.IEventReceiver;

public class SaveSyncEventHandlers implements IEventReceiver {

	@EventListener
	public void authenticatedSave(AccountSaveAuthenticatedEvent ev) {
		GridSaveUtil.updateGridSaveID(ev.getSave());
	}

	@EventListener
	public void updateSaveUsername(AccountSaveUsernameUpdateEvent ev) {
		GridSaveUtil.updateGridSaveUsername(ev.getSave(), ev.getSave().getUsername());
	}

	@EventListener
	public void createdSave(AccountSaveCreatedEvent ev) {
		GridSaveUtil.updateGridSaveID(ev.getSave());
	}

	@EventListener
	public void deletedSave(AccountSaveDeletedEvent ev) {
		GridSaveUtil.deleteGridSaveOf(ev.getSave());
	}

}
