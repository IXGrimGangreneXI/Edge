package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.webservices.annotations.AccountKvContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class AccountKvContainerAnnoProcessor implements IParameterAnnotationProcessor<AccountKvContainer> {

	@Override
	public Class<AccountKvContainer> annotation() {
		return AccountKvContainer.class;
	}

	@Override
	public boolean match(AccountKvContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return function.getProcessorMemoryObject(AccountObject.class) != null
				&& param.getType().isAssignableFrom(AccountKvDataContainer.class);
	}

	@Override
	public Object process(AccountKvContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return function.getProcessorMemoryObject(AccountObject.class).getAccountKeyValueContainer(annotation.value());
	}

}
