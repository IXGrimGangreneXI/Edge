package org.asf.edge.common.webservices.processors;

import java.io.IOException;
import java.lang.reflect.Method;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.webservices.SodRequestInfo;
import org.asf.edge.common.webservices.annotations.SodTokenSecured;
import org.asf.edge.common.webservices.annotations.TokenRequireCapabilities;
import org.asf.edge.common.webservices.annotations.TokenRequireCapability;
import org.asf.edge.common.webservices.annotations.TokenRequireSave;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IMethodAnnotationProcessor;
import org.asf.nexus.webservices.functions.processors.MatchResult;
import org.bouncycastle.util.encoders.Base32;

public class SodTokenSecuredAnnoProcessor implements IMethodAnnotationProcessor<SodTokenSecured> {

	@Override
	public Class<SodTokenSecured> annotation() {
		return SodTokenSecured.class;
	}

	@Override
	public MatchResult process(SodTokenSecured annotation, Method meth, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		// Retrieve request information
		SodRequestInfo req = SodRequestInfo.getOrCreateFrom(function, apiRequestParams, webservice);

		// Prepare
		SessionToken tkn = null;
		AccountObject account = null;
		AccountSaveContainer save = null;

		// Check token
		if (!req.payload.has("apiToken"))
			throw new HttpException(400, "Bad request");
		else {
			try {
				// Retrieve token
				String apiToken = new String(Base32.decode(req.payload.getString("apiToken").toUpperCase()), "UTF-8");

				// Read token
				tkn = new SessionToken();
				TokenParseResult res = tkn.parseToken(apiToken);
				account = tkn.account;
				if (res != TokenParseResult.SUCCESS) {
					// Error
					tkn = null;
					account = null;
					throw new HttpException(401, "Unauthorized");
				}

				// Verify capabilities
				if (tkn != null && meth.isAnnotationPresent(TokenRequireCapabilities.class)) {
					TokenRequireCapabilities caps = meth.getAnnotation(TokenRequireCapabilities.class);
					for (TokenRequireCapability c : caps.value()) {
						if (!tkn.hasCapability(c.value())) {
							tkn = null;
							account = null;
							throw new HttpException(401, "Unauthorized");
						}
					}
				}

				// Check
				if (tkn != null) {
					if (tkn.saveID != null)
						save = account.getSave(tkn.saveID);
					else if (meth.isAnnotationPresent(TokenRequireSave.class)) {
						tkn = null;
						account = null;
						throw new HttpException(401, "Unauthorized");
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// Assign result
		function.setProcessorMemoryObject(SessionToken.class, tkn);
		function.setProcessorMemoryObject(AccountObject.class, account);
		if (save != null)
			function.setProcessorMemoryObject(AccountSaveContainer.class, save);

		// Return success
		return MatchResult.SUCCESS;
	}

}
