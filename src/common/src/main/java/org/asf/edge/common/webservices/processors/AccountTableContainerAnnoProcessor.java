package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.webservices.annotations.AccountTableContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class AccountTableContainerAnnoProcessor implements IParameterAnnotationProcessor<AccountTableContainer> {

	@Override
	public Class<AccountTableContainer> annotation() {
		return AccountTableContainer.class;
	}

	@Override
	public boolean match(AccountTableContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(AccountObject.class) != null
				&& param.getType().isAssignableFrom(AccountDataTableContainer.class);
	}

	@Override
	public Object process(AccountTableContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return function.getProcessorMemoryObject(AccountObject.class).getAccountDataTable(annotation.name(),
				annotation.rowType());
	}

}
