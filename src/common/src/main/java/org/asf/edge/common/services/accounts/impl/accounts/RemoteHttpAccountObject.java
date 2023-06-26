package org.asf.edge.common.services.accounts.impl.accounts;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.RemoteHttpAccountManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class RemoteHttpAccountObject extends AccountObject {

	private String id;
	private String username;
	private RemoteHttpAccountManager mgr;

	private Logger logger = LogManager.getLogger("AccountManager");

	public RemoteHttpAccountObject(String id, String username, RemoteHttpAccountManager mgr) {
		this.id = id;
		this.username = username;
		this.mgr = mgr;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getAccountID() {
		return id;
	}

	@Override
	public long getLastLoginTime() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/getLastLoginTime", payload);
			return response.get("time").getAsLong();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getLastLoginTime!", e);
			return -1;
		}
	}

	@Override
	public String getAccountEmail() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/getAccountEmail", payload);
			return response.get("email").getAsString();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getAccountEmail!", e);
			return null;
		}
	}

	@Override
	public long getRegistrationTimestamp() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/getRegistrationTimestamp", payload);
			return response.get("time").getAsLong();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getRegistrationTimestamp!", e);
			return -1;
		}
	}

	@Override
	public boolean updateUsername(String name) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("newName", name);
			JsonObject response = mgr.accountManagerRequest("accounts/updateUsername", payload);
			boolean res = response.get("success").getAsBoolean();
			if (res)
				username = name;
			return res;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in updateUsername!", e);
			return false;
		}
	}

	@Override
	public boolean updateEmail(String email) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("email", email);
			JsonObject response = mgr.accountManagerRequest("accounts/updateEmail", payload);
			return response.get("success").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in updateEmail!", e);
			return false;
		}
	}

	@Override
	public boolean updatePassword(char[] newPassword) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("newPassword", new String(newPassword));
			JsonObject response = mgr.accountManagerRequest("accounts/updatePassword", payload);
			return response.get("success").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in updatePassword!", e);
			return false;
		}
	}

	@Override
	public boolean migrateToNormalAccountFromGuest(String newName, String email, char[] password) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("newName", newName);
			payload.addProperty("email", email);
			payload.addProperty("password", new String(password));
			JsonObject response = mgr.accountManagerRequest("accounts/migrateToNormalAccountFromGuest", payload);
			return response.get("success").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in migrateToNormalAccountFromGuest!", e);
			return false;
		}
	}

	@Override
	public boolean isGuestAccount() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/isGuestAccount", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isGuestAccount!", e);
			return false;
		}
	}

	@Override
	public boolean isMultiplayerEnabled() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/isMultiplayerEnabled", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isMultiplayerEnabled!", e);
			return false;
		}
	}

	@Override
	public boolean isChatEnabled() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/isChatEnabled", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isChatEnabled!", e);
			return false;
		}
	}

	@Override
	public boolean isStrictChatFilterEnabled() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/isStrictChatFilterEnabled", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isStrictChatFilterEnabled!", e);
			return false;
		}
	}

	@Override
	public void setMultiplayerEnabled(boolean state) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("state", state);
			JsonObject response = mgr.accountManagerRequest("accounts/setMultiplayerEnabled", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in setMultiplayerEnabled!", e);
		}
	}

	@Override
	public void setChatEnabled(boolean state) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("state", state);
			JsonObject response = mgr.accountManagerRequest("accounts/setChatEnabled", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in setChatEnabled!", e);
		}
	}

	@Override
	public void setStrictChatFilterEnabled(boolean state) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("state", state);
			JsonObject response = mgr.accountManagerRequest("accounts/setStrictChatFilterEnabled", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in setStrictChatFilterEnabled!", e);
		}
	}

	@Override
	public void updateLastLoginTime() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/updateLastLoginTime", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in updateLastLoginTime!", e);
		}
	}

	@Override
	public void deleteAccount() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/deleteAccount", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in deleteAccount!", e);
		}
	}

	@Override
	public AccountDataContainer getAccountData() {
		return new RemoteHttpDataContainer(id, mgr);
	}

	@Override
	public String[] getSaveIDs() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = mgr.accountManagerRequest("accounts/getSaveIDs", payload);
			JsonArray arr = response.get("saves").getAsJsonArray();
			String[] saves = new String[arr.size()];
			int i = 0;
			for (JsonElement ele : arr)
				saves[i++] = ele.getAsString();
			return saves;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getSaveIDs!", e);
			return new String[0];
		}
	}

	@Override
	public AccountSaveContainer createSave(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("username", username);
			JsonObject response = mgr.accountManagerRequest("accounts/createSave", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			String id = response.get("id").getAsString();
			return new RemoteHttpSaveContainer(id, response.get("time").getAsLong(), username, this.id, mgr, this);
		} catch (IOException e) {
			logger.error("Account server query failure occurred in createSave!", e);
			return null;
		}
	}

	@Override
	public AccountSaveContainer getSave(String id) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", this.id);
			payload.addProperty("save", id);
			JsonObject response = mgr.accountManagerRequest("accounts/getSave", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return new RemoteHttpSaveContainer(id, response.get("time").getAsLong(),
					response.get("username").getAsString(), this.id, mgr, this);
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getSave!", e);
			return null;
		}
	}

}
