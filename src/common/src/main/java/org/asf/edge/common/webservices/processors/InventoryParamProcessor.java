package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterProcessor;

public class InventoryParamProcessor implements IParameterProcessor {

	@Override
	public boolean match(Method meth, Class<?> paramType, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(SessionToken.class) != null
				&& param.getType().isAssignableFrom(PlayerInventory.class);
	}

	@Override
	public Object process(Method meth, Class<?> paramType, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		AccountSaveContainer save = function.getProcessorMemoryObject(AccountSaveContainer.class);
		if (save != null)
			return save.getInventory();
		return function.getProcessorMemoryObject(AccountObject.class).getInventory();
	}

}
