package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.webservices.annotations.SaveTableContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class SaveTableContainerAnnoProcessor implements IParameterAnnotationProcessor<SaveTableContainer> {

	@Override
	public Class<SaveTableContainer> annotation() {
		return SaveTableContainer.class;
	}

	@Override
	public boolean match(SaveTableContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(AccountSaveContainer.class) != null
				&& param.getType().isAssignableFrom(AccountDataTableContainer.class);
	}

	@Override
	public Object process(SaveTableContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return function.getProcessorMemoryObject(AccountSaveContainer.class).getSaveDataTable(annotation.name(),
				annotation.rowType());
	}

}
