package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.webservices.annotations.KvContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class KvContainerAnnoProcessor implements IParameterAnnotationProcessor<KvContainer> {

	@Override
	public Class<KvContainer> annotation() {
		return KvContainer.class;
	}

	@Override
	public boolean match(KvContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return (function.getProcessorMemoryObject(AccountSaveContainer.class) != null
				|| function.getProcessorMemoryObject(AccountObject.class) != null)
				&& param.getType().isAssignableFrom(AccountKvDataContainer.class);
	}

	@Override
	public Object process(KvContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		AccountSaveContainer save = function.getProcessorMemoryObject(AccountSaveContainer.class);
		if (save == null)
			return function.getProcessorMemoryObject(AccountObject.class)
					.getAccountKeyValueContainer(annotation.value());
		return save.getSaveKeyValueContainer(annotation.value());
	}

}
