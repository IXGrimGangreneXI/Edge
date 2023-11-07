package org.asf.edge.modules.gridapi.http.services;

import java.io.IOException;
import java.util.UUID;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.common.webservices.annotations.Function;
import org.asf.edge.common.webservices.annotations.FunctionInfo;
import org.asf.edge.common.webservices.annotations.FunctionResult;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;
import org.asf.edge.modules.gridapi.utils.TokenUtils;

public class GridUtilityWebService extends EdgeWebService<EdgeGridApiServer> {

	public GridUtilityWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new GridUtilityWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/grid/utilities";
	}

	@Function
	public FunctionResult testConnection(FunctionInfo func) {
		return ok("text/plain", "OK");
	}

	@Function
	public FunctionResult testSessionLock(FunctionInfo func) {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func);
		if (ctx == null) {
			return response(401, "Unauthorized", "text/plain", "TOKEN_INVALID");
		}
		PhoenixToken jwt = ctx.token;

		// Check significant fields
		if (!jwt.identity.equals(new UUID(0, 0).toString()) && jwt.payload != null && jwt.payload.isJsonObject()
				&& jwt.payload.getAsJsonObject().has("isfr") && jwt.payload.getAsJsonObject().has("isfn")) {
			try {
				// Identity-based token
				if (ctx.account != null) {
					// Load data
					AccountKvDataContainer data = ctx.account.getAccountKeyValueContainer().getChildContainer("accountdata");
					if (!data.entryExists("significantFieldRandom"))
						return ok("text/json", "LOCK_HELD");
					if (!data.entryExists("significantFieldNumber"))
						return ok("text/json", "LOCK_HELD");

					// Check fields
					long isfn = data.getEntry("significantFieldNumber").getAsLong();
					int isfr = data.getEntry("significantFieldRandom").getAsInt();
					if (isfn != jwt.payload.getAsJsonObject().get("isfn").getAsLong()
							|| isfr != jwt.payload.getAsJsonObject().get("isfr").getAsInt()) {
						return ok("text/json", "LOCK_LOST");
					}
				} else if (ctx.identity != null) {
					// Check fields
					if (ctx.identity.significantFieldNumber != jwt.payload.getAsJsonObject().get("isfn").getAsLong()
							|| ctx.identity.significantFieldRandom != jwt.payload.getAsJsonObject().get("isfr")
									.getAsInt()) {
						return ok("text/json", "LOCK_LOST");
					}
				} else
					return ok("text/json", "LOCK_LOST");
			} catch (IOException e) {
				return ok("text/json", "LOCK_LOST");
			}
		}

		return ok("text/json", "LOCK_HELD");
	}

}
