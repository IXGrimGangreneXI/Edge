package org.asf.edge.modules.gridapi.phoenixconnector.handlers;

import java.util.function.Consumer;

import org.asf.connective.RemoteClient;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.textfilter.result.FilterResult;
import org.asf.edge.common.services.textfilter.result.WordMatch;
import org.asf.edge.modules.gridapi.phoenixconnector.IApiConnectorRequestHandler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TextFilterRequestHandler implements IApiConnectorRequestHandler {

	@Override
	public String requestType() {
		return "TextFilter";
	}

	@Override
	public void handle(JsonObject payload, RemoteClient client, Consumer<JsonObject> responseCallback) {
		// Read
		String message = payload.get("message").getAsString();
		boolean strictChat = payload.get("strictChat").getAsBoolean();

		// Filter
		FilterResult result = TextFilterService.getInstance().filter(message, strictChat);

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
		res.addProperty("filteredResult", result.getFilterResult());
		res.addProperty("severity", result.getSeverity().toString());
		res.add("matches", matches);

		// Send
		responseCallback.accept(res);
	}

}
