package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.webservices.annotations.TableContainer;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;

public class TableContainerAnnoProcessor implements IParameterAnnotationProcessor<TableContainer> {

	@Override
	public Class<TableContainer> annotation() {
		return TableContainer.class;
	}

	@Override
	public boolean match(TableContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return (function.getProcessorMemoryObject(AccountSaveContainer.class) != null
				|| function.getProcessorMemoryObject(AccountObject.class) != null)
				&& param.getType().isAssignableFrom(AccountDataTableContainer.class);
	}

	@Override
	public Object process(TableContainer annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		AccountSaveContainer save = function.getProcessorMemoryObject(AccountSaveContainer.class);
		if (save == null)
			return function.getProcessorMemoryObject(AccountObject.class).getAccountDataTable(annotation.name(),
					annotation.rowType());
		return save.getSaveDataTable(annotation.name(), annotation.rowType());
	}

}
