package org.asf.edge.commonapi.http.handlers.api.edgespecific;

import java.io.IOException;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.textfilter.result.FilterResult;
import org.asf.edge.common.services.textfilter.result.WordMatch;
import org.asf.edge.commonapi.EdgeCommonApiServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EdgeTextFilterApiService extends EdgeWebService<EdgeCommonApiServer> {

	public EdgeTextFilterApiService(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new EdgeTextFilterApiService(getServerInstance());
	}

	@Override
	public String path() {
		return "/API/ProjectEdge/TextFilter/";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult filterText(FunctionInfo func) {
		// Edge text filter

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("message") || !request.has("strictMode"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request");
		}

		// Run filter
		String message = request.get("message").getAsString();
		boolean strictMode = request.get("strictMode").getAsBoolean();
		FilterResult result = TextFilterService.getInstance().filter(message, strictMode);

		// Build response match list
		JsonArray matches = new JsonArray();
		for (WordMatch match : result.getMatches()) {
			JsonObject m = new JsonObject();
			m.addProperty("phrase", match.getPhraseFilter().getPhrase());
			m.addProperty("matchedPhrase", match.getMatchedPhrase());
			m.addProperty("reason", match.getReason());
			m.addProperty("severity", match.getSeverity().toString());
			matches.add(m);
		}

		// Build response
		JsonObject res = new JsonObject();
		res.addProperty("isFiltered", result.isMatch());
		res.addProperty("resultMessage", result.getFilterResult());
		res.addProperty("resultSeverity", result.getSeverity().toString());
		res.add("matches", matches);

		// Return result
		return ok("text/json", res.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult isFiltered(FunctionInfo func) {
		// Edge text filter

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("message") || !request.has("strictMode"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request");
		}

		// Build response
		JsonObject res = new JsonObject();
		String message = request.get("message").getAsString();
		boolean strictMode = request.get("strictMode").getAsBoolean();
		res.addProperty("result", TextFilterService.getInstance().isFiltered(message, strictMode));

		// Return result
		return ok("text/json", res.toString());
	}

}
