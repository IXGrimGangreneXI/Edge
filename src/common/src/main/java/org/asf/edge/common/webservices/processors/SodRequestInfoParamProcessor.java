package org.asf.edge.common.webservices.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.webservices.SodRequestInfo;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.processors.IParameterProcessor;

public class SodRequestInfoParamProcessor implements IParameterProcessor {

	@Override
	public boolean match(Method meth, Class<?> paramType, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		return paramType.isAssignableFrom(SodRequestInfo.class);
	}

	@Override
	public Object process(Method meth, Class<?> paramType, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return SodRequestInfo.getOrCreateFrom(function, apiRequestParams, webservice);
	}

}
