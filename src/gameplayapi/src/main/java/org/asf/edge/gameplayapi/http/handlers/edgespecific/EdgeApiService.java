package org.asf.edge.gameplayapi.http.handlers.edgespecific;

import java.io.IOException;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.textfilter.result.FilterResult;
import org.asf.edge.common.services.textfilter.result.WordMatch;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Edge API endpoint, holds public edge-specific API features
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeApiService extends EdgeWebService<EdgeGameplayApiServer> {

	public EdgeApiService(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new EdgeApiService(getServerInstance());
	}

	@Override
	public String path() {
		return "/API/ProjectEdge/";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult textFilter(FunctionInfo func) {
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

}
