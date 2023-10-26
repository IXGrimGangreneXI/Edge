package org.asf.edge.modules.gridapi.utils;

import java.io.IOException;
import java.util.UUID;

import org.asf.edge.common.http.functions.FunctionInfo;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.identities.IdentityDef;

import com.google.gson.JsonPrimitive;

public class TokenUtils {

	public static class AccessContext {
		public PhoenixToken token;
		public IdentityDef identity;
		public AccountObject account;
		public boolean isServer;
		public boolean isAccount;
	}

	/**
	 * Retrieves a Phoenix API token from a API function object
	 * 
	 * @param func Function info object
	 * @param cap  Required capability
	 * @return AccessContext instance or null
	 */
	public static AccessContext fromFunction(FunctionInfo func) {
		// Check authentication header
		String auth = func.getRequest().getHeaderValue("Authorization");
		if (auth == null || !auth.startsWith("Bearer ")) {
			func.getResponse().setResponseStatus(401, "Unauthorized");
			return null;
		}

		// Load token
		String tokenR = auth.substring("Bearer ".length());
		AccessContext res = fromToken(tokenR);
		if (res == null) {
			func.getResponse().setResponseStatus(401, "Unauthorized");
			return null;
		}

		// Verify server
		if (res.token != null && res.token.hasCapability("serveraccess")) {
			// Retrieve ID from token
			String sid = res.token.payload.getAsJsonObject().get("sid").getAsString();

			// Check authentication header
			String authServer = func.getRequest().getHeaderValue("Server-Authorization");
			if (authServer == null || !authServer.startsWith("Bearer ")) {
				func.getResponse().setResponseStatus(401, "Unauthorized");
				return null;
			}

			// Load token
			String tokenRS = authServer.substring("Bearer ".length());
			AccessContext resS = fromToken(tokenRS, "host");
			if (resS == null) {
				func.getResponse().setResponseStatus(401, "Unauthorized");
				return null;
			}

			// Verify ID
			if (!resS.identity.identity.equals(sid)) {
				func.getResponse().setResponseStatus(401, "Unauthorized");
				return null;
			}
		}

		// Return
		return res;
	}

	/**
	 * Retrieves a Phoenix API token from a API function object
	 * 
	 * @param func Function info object
	 * @param cap  Required capability
	 * @return AccessContext instance or null
	 */
	public static AccessContext fromFunction(FunctionInfo func, String cap) {
		// Check authentication header
		String auth = func.getRequest().getHeaderValue("Authorization");
		if (auth == null || !auth.startsWith("Bearer ")) {
			func.getResponse().setResponseStatus(401, "Unauthorized");
			return null;
		}

		// Load token
		String tokenR = auth.substring("Bearer ".length());
		AccessContext res = fromToken(tokenR, cap);
		if (res == null) {
			func.getResponse().setResponseStatus(401, "Unauthorized");
			return null;
		}

		// Verify server
		if (res.token != null && res.token.hasCapability("serveraccess")) {
			// Retrieve ID from token
			String sid = res.token.payload.getAsJsonObject().get("sid").getAsString();

			// Check authentication header
			String authServer = func.getRequest().getHeaderValue("Server-Authorization");
			if (authServer == null || !authServer.startsWith("Bearer ")) {
				func.getResponse().setResponseStatus(401, "Unauthorized");
				return null;
			}

			// Load token
			String tokenRS = authServer.substring("Bearer ".length());
			AccessContext resS = fromToken(tokenRS, "host");
			if (resS == null) {
				func.getResponse().setResponseStatus(401, "Unauthorized");
				return null;
			}

			// Verify ID
			if (!resS.identity.identity.equals(sid)) {
				func.getResponse().setResponseStatus(401, "Unauthorized");
				return null;
			}
		}

		// Return
		return res;
	}

	/**
	 * Retrieves AccessContext from a Phoenix API token
	 * 
	 * @param tokenStr Token string
	 * @return AccessContext instance or null
	 */
	public static AccessContext fromToken(String tokenStr) {
		return fromToken(tokenStr, null);
	}

	/**
	 * Retrieves AccessContext from a Phoenix API token
	 * 
	 * @param tokenStr Token string
	 * @param cap      Required capability
	 * @return AccessContext instance or null
	 */
	public static AccessContext fromToken(String tokenStr, String cap) {
		PhoenixToken tkn = new PhoenixToken();
		if (tkn.parseToken(tokenStr) != TokenParseResult.SUCCESS || (cap != null && !tkn.hasCapability(cap))) {
			return null;
		}

		// Find identity
		AccessContext ctx = new AccessContext();
		ctx.token = tkn;
		if (tkn.identity.equals(new UUID(0, 0).toString())) {
			ctx.identity = IdentityUtils.getIdentity(new UUID(0, 0).toString());
			if (ctx.identity == null) {
				IdentityDef def = new IdentityDef();
				def.identity = new UUID(0, 0).toString();
				IdentityUtils.updateIdentity(def);
				ctx.identity = def;
			}
			return ctx;
		} else if (!AccountManager.getInstance().accountExists(tkn.identity)) {
			IdentityDef id = IdentityUtils.getIdentity(tkn.identity);
			if (id == null) {
				return null;
			}
			if (id.lastUpdateTime != tkn.lastUpdate) {
				return null;
			}
			ctx.identity = id;
		} else {
			// Load account
			AccountObject acc = AccountManager.getInstance().getAccount(tkn.identity);
			if (acc == null) {
				return null;
			}

			// Load last update
			try {
				AccountKvDataContainer data = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
				if (!data.entryExists("last_update"))
					data.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));
				if (data.getEntry("last_update").getAsLong() != tkn.lastUpdate) {
					return null;
				}
			} catch (IOException e) {
				return null;
			}

			// Apply
			ctx.account = acc;
			ctx.identity = IdentityUtils.getIdentity(tkn.identity);
			ctx.isAccount = true;
		}
		ctx.isServer = ctx.identity.properties.containsKey("serverCertificateProperties");

		// Check
		if (tkn.hasCapability("serveraccess")) {
			// Verify account
			if (ctx.account == null)
				return null;

			// Verify server ID property
			if (tkn.payload == null || !tkn.payload.isJsonObject() || !tkn.payload.getAsJsonObject().has("sid"))
				return null;

			// Retrieve ID from token
			String sid = tkn.payload.getAsJsonObject().get("sid").getAsString();

			// Verify ID
			try {
				AccountKvDataContainer data = ctx.account.getAccountKeyValueContainer().getChildContainer("accountdata");
				if (!data.entryExists("current_server") || !data.getEntry("current_server").getAsString().equals(sid))
					return null;
			} catch (IOException e) {
				return null;
			}
		}

		// Valid
		return ctx;
	}

}
