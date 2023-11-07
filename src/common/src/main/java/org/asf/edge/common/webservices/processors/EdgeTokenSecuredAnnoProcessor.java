package org.asf.edge.common.webservices.processors;

import java.io.IOException;
import java.lang.reflect.Method;

import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.webservices.annotations.EdgeTokenSecured;
import org.asf.edge.common.webservices.annotations.EdgeTokenRequireCapabilities;
import org.asf.edge.common.webservices.annotations.EdgeTokenRequireCapability;
import org.asf.edge.common.webservices.annotations.EdgeTokenRequireSave;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IMethodAnnotationProcessor;
import org.asf.nexus.webservices.functions.processors.MatchResult;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.bouncycastle.util.encoders.Base32;

public class EdgeTokenSecuredAnnoProcessor implements IMethodAnnotationProcessor<EdgeTokenSecured> {

	@Override
	public Class<EdgeTokenSecured> annotation() {
		return EdgeTokenSecured.class;
	}

	@Override
	public MatchResult process(EdgeTokenSecured annotation, Method meth, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		// Prepare
		SessionToken tkn = null;
		AccountObject account = null;
		AccountSaveContainer save = null;

		// Check token
		try {
			// Retrieve token
			String apiToken = getToken(function, apiRequestParams);
			if (apiToken == null)
				throw new HttpException(400, "Bad request");

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
			if (tkn != null && meth.isAnnotationPresent(EdgeTokenRequireCapabilities.class)) {
				EdgeTokenRequireCapabilities caps = meth.getAnnotation(EdgeTokenRequireCapabilities.class);
				for (EdgeTokenRequireCapability c : caps.value()) {
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
				else if (meth.isAnnotationPresent(EdgeTokenRequireSave.class)) {
					tkn = null;
					account = null;
					throw new HttpException(401, "Unauthorized");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Assign result
		function.setProcessorMemoryObject(SessionToken.class, tkn);
		function.setProcessorMemoryObject(AccountObject.class, account);
		if (save != null)
			function.setProcessorMemoryObject(AccountSaveContainer.class, save);

		// Return success
		return MatchResult.SUCCESS;
	}

	private String getToken(FunctionInfo function, ApiRequestParams apiRequestParams) throws IOException {
		// Check
		if (apiRequestParams.has("apiToken"))
			return new String(Base32.decode(apiRequestParams.getString("apiToken").toUpperCase()), "UTF-8");
		else if (apiRequestParams.has("edgeSessionToken"))
			return apiRequestParams.getString("edgeSessionToken");
		else if (function.getRequest().hasHeader("Authorization")
				&& function.getRequest().getHeaderValue("Authorization").toLowerCase().startsWith("bearer "))
			return function.getRequest().getHeaderValue("Authorization").substring("Bearer ".length());

		// No token
		return null;
	}

}
