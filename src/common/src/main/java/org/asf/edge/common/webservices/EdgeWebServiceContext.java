package org.asf.edge.common.webservices;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.asf.edge.common.IEdgeBaseServer;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.webservices.annotations.AccountInventory;
import org.asf.edge.common.webservices.annotations.AccountKvContainer;
import org.asf.edge.common.webservices.annotations.AccountTableContainer;
import org.asf.edge.common.webservices.annotations.SaveInventory;
import org.asf.edge.common.webservices.annotations.SaveKvContainer;
import org.asf.edge.common.webservices.annotations.SaveTableContainer;
import org.asf.edge.common.webservices.annotations.SodEncryptedParam;
import org.asf.edge.common.webservices.annotations.SodTokenSecured;
import org.asf.edge.common.webservices.annotations.TokenRequireCapabilities;
import org.asf.edge.common.webservices.annotations.TokenRequireCapability;
import org.asf.edge.common.webservices.annotations.TokenRequireSave;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.bouncycastle.util.encoders.Base32;

/**
 * 
 * Edge Webservice Context Container
 * 
 * @author Sky Swimmer
 * 
 */
public class EdgeWebServiceContext<T extends IEdgeBaseServer> {

	private Object[] populateArguments(Method mth, FunctionInfo fI, SodRequestInfo req) throws IOException {
		// Create array
		Parameter[] params = mth.getParameters();
		Object[] args = new Object[params.length];
		HttpException pendingException = null;

		// Go through parameters
		for (int i = 0; i < args.length; i++) {
			// Check
			Parameter param = params[i];

			// Check type
			if (param.getType().isAssignableFrom(AccountKvDataContainer.class)
					&& param.isAnnotationPresent(AccountKvContainer.class)) {
				if (account != null)
					args[i] = account
							.getAccountKeyValueContainer(param.getAnnotation(AccountKvContainer.class).value());
				else
					pendingException = new HttpException(400, "Bad request");
			} else if (param.getType().isAssignableFrom(AccountKvDataContainer.class)
					&& param.isAnnotationPresent(SaveKvContainer.class)) {
				if (save != null)
					args[i] = save.getSaveKeyValueContainer(param.getAnnotation(SaveKvContainer.class).value());
				else
					pendingException = new HttpException(400, "Bad request");
			} else if (param.getType().isAssignableFrom(AccountDataTableContainer.class)
					&& param.isAnnotationPresent(AccountTableContainer.class)) {
				AccountTableContainer anno = param.getAnnotation(AccountTableContainer.class);
				if (account != null)
					args[i] = account.getAccountDataTable(anno.name(), anno.rowType());
				else
					pendingException = new HttpException(400, "Bad request");
			} else if (param.getType().isAssignableFrom(AccountDataTableContainer.class)
					&& param.isAnnotationPresent(SaveTableContainer.class)) {
				SaveTableContainer anno = param.getAnnotation(SaveTableContainer.class);
				if (save != null)
					args[i] = save.getSaveDataTable(anno.name(), anno.rowType());
				else
					pendingException = new HttpException(400, "Bad request");
			} else if (param.isAnnotationPresent(SodRequestParam.class)) {
				// Handle param
				String name = param.getAnnotation(SodRequestParam.class).value();
				if (name.isEmpty())
					name = param.getName();

				// Retrieve value
				String val = req.payload.get(name);
				if (val == null)
					pendingException = new HttpException(400, "Bad request");
				else {
					// Decrypt if needed
					try {
						// Decrypt if needed
						if (param.isAnnotationPresent(SodEncryptedParam.class))
							val = req.decryptString(val);
					} catch (Exception e) {
						pendingException = new HttpException(400, "Bad request");
					}
				}
			} else
				throw new RuntimeException("Invalid parameter " + param.getName() + " in method " + mth.getName()
						+ " of " + getClass().getTypeName() + "!");
		}

		// Check exception
		if (pendingException != null)
			throw pendingException;

		// Return
		return args;

	}
}
