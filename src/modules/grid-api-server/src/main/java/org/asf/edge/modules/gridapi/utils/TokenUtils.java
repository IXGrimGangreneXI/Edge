package org.asf.edge.modules.gridapi.utils;

import java.io.IOException;
import java.util.UUID;

import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.identities.IdentityDef;

import com.google.gson.JsonPrimitive;

public class TokenUtils {

	private static final String NIL_UUID = new UUID(0, 0).toString();

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
		if (res == null)
			func.getResponse().setResponseStatus(401, "Unauthorized");
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
		if (res == null)
			func.getResponse().setResponseStatus(401, "Unauthorized");
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
		if (tkn.parseToken(tokenStr) != TokenParseResult.SUCCESS && (cap != null && !tkn.hasCapability(cap))) {
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
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
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

		// Check significant fields
		if (!tkn.identity.equals(NIL_UUID) && tkn.payload != null && tkn.payload.isJsonObject()
				&& tkn.payload.getAsJsonObject().has("isfr") && tkn.payload.getAsJsonObject().has("isfn")) {
			try {
				// Identity-based token
				if (ctx.account != null) {
					// Load data
					AccountDataContainer data = ctx.account.getAccountData().getChildContainer("accountdata");
					if (!data.entryExists("significantFieldRandom"))
						return null;
					if (!data.entryExists("significantFieldNumber"))
						return null;

					// Check fields
					long isfn = data.getEntry("significantFieldNumber").getAsLong();
					int isfr = data.getEntry("significantFieldRandom").getAsInt();
					if (isfn != tkn.payload.getAsJsonObject().get("isfn").getAsLong()
							|| isfr != tkn.payload.getAsJsonObject().get("isfr").getAsInt()) {
						return null;
					}
				} else if (ctx.identity != null) {
					// Check fields
					if (ctx.identity.significantFieldNumber != tkn.payload.getAsJsonObject().get("isfn").getAsLong()
							|| ctx.identity.significantFieldRandom != tkn.payload.getAsJsonObject().get("isfr")
									.getAsInt()) {
						return null;
					}
				} else
					return null;
			} catch (IOException e) {
				return null;
			}
		}

		// Valid
		return ctx;
	}

}
