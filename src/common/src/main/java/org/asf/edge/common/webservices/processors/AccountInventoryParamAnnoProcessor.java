package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.webservices.annotations.AccountInventory;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class AccountInventoryParamAnnoProcessor implements IParameterAnnotationProcessor<AccountInventory> {

	@Override
	public Class<AccountInventory> annotation() {
		return AccountInventory.class;
	}

	@Override
	public boolean match(AccountInventory annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(AccountObject.class) != null
				&& param.getType().isAssignableFrom(PlayerInventory.class);
	}

	@Override
	public Object process(AccountInventory annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return function.getProcessorMemoryObject(AccountObject.class).getInventory();
	}

}
