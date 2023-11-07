package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterProcessor;

public class SessionObjectsParamProcessor implements IParameterProcessor {

	@Override
	public boolean match(Method meth, Class<?> paramType, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		// Check type and objects
		if (paramType.isAssignableFrom(AccountObject.class)
				&& function.getProcessorMemoryObject(AccountObject.class) != null)
			return true;
		else if (paramType.isAssignableFrom(AccountSaveContainer.class)
				&& function.getProcessorMemoryObject(AccountSaveContainer.class) != null)
			return true;
		else if (paramType.isAssignableFrom(AccountSaveContainer.class)
				&& function.getProcessorMemoryObject(AccountSaveContainer.class) != null)
			return true;

		// Invalid
		return false;
	}

	@Override
	public Object process(Method meth, Class<?> paramType, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		// Check type and return value
		if (paramType.isAssignableFrom(AccountObject.class))
			return function.getProcessorMemoryObject(AccountObject.class);
		else if (paramType.isAssignableFrom(AccountSaveContainer.class))
			return function.getProcessorMemoryObject(AccountSaveContainer.class);
		else if (paramType.isAssignableFrom(AccountSaveContainer.class))
			return function.getProcessorMemoryObject(AccountSaveContainer.class);

		// Invalid
		return null;
	}

}
