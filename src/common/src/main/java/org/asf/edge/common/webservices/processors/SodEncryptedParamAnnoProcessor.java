package org.asf.edge.common.webservices.processors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.asf.edge.common.webservices.SodRequestInfo;
import org.asf.edge.common.webservices.annotations.SodEncryptedParam;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.ApiRequestParams;
import org.asf.nexus.webservices.exceptions.HttpException;
import org.asf.nexus.webservices.functions.FunctionInfo;
import org.asf.nexus.webservices.functions.annotations.RequestParam;
import org.asf.nexus.webservices.functions.processors.IParameterAnnotationProcessor;
import org.asf.nexus.webservices.requestparams.impl.UrlEncodedParamsProvider;

public class SodEncryptedParamAnnoProcessor implements IParameterAnnotationProcessor<SodEncryptedParam> {

	@Override
	public Class<SodEncryptedParam> annotation() {
		return SodEncryptedParam.class;
	}

	@Override
	public boolean match(SodEncryptedParam annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) {
		// Check annotation
		if (param.isAnnotationPresent(RequestParam.class)) {
			// Handle param
			RequestParam anno = param.getAnnotation(RequestParam.class);
			String name = anno.value();
			if (name.isEmpty())
				name = param.getName();

			try {
				// Decrypt
				SodRequestInfo req = SodRequestInfo.getOrCreateFrom(function, apiRequestParams, webservice);
				String dec = req.decryptString(apiRequestParams.getString(name));

				// Update to use form method as that parses the objects from strings
				apiRequestParams.setProvider(name, new UrlEncodedParamsProvider(dec));
			} catch (IOException e) {
				throw new HttpException(400, "Bad Request");
			}
		}

		// Return false so that the system continues
		return false;
	}

	@Override
	public Object process(SodEncryptedParam annotation, Method meth, Parameter param, FunctionInfo function,
			ApiRequestParams apiRequestParams, AbstractWebService<?> webservice) throws HttpException {
		return null;
	}

}
