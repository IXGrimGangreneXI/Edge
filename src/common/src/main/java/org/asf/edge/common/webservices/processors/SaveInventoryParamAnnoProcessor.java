package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.webservices.annotations.SaveInventory;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class SaveInventoryParamAnnoProcessor implements IParameterAnnotationProcessor<SaveInventory> {

	@Override
	public Class<SaveInventory> annotation() {
		return SaveInventory.class;
	}

	@Override
	public boolean match(SaveInventory annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(AccountSaveContainer.class) != null
				&& param.getType().isAssignableFrom(PlayerInventory.class);
	}

	@Override
	public Object process(SaveInventory annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return function.getProcessorMemoryObject(AccountSaveContainer.class).getInventory();
	}

}
