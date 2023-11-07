package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.webservices.annotations.SaveKvContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class SaveKvContainerAnnoProcessor implements IParameterAnnotationProcessor<SaveKvContainer> {

	@Override
	public Class<SaveKvContainer> annotation() {
		return SaveKvContainer.class;
	}

	@Override
	public boolean match(SaveKvContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(AccountSaveContainer.class) != null
				&& param.getType().isAssignableFrom(AccountKvDataContainer.class);
	}

	@Override
	public Object process(SaveKvContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return function.getProcessorMemoryObject(AccountSaveContainer.class)
				.getSaveKeyValueContainer(annotation.value());
	}

}
